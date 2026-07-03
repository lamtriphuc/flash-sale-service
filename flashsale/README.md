# FlashDeal - Flash Sale Backend

FlashDeal is a backend-focused flash sale system built to demonstrate high-concurrency order handling, cache-first inventory control, asynchronous order processing, and production-style infrastructure design. The project is intentionally backend-only; no frontend is required.

The system is single-tenant at runtime. Core tables still keep `tenant_id` as an internal field so the schema can be expanded later, but the current API does not implement real multi-tenant routing or API-key based tenant authentication.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x |
| Security | Spring Security, JWT access token, JWT refresh token |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Cache / Concurrency Gate | Redis, Lua script for atomic stock reservation |
| Async Processing | RabbitMQ, Spring AMQP |
| Realtime | WebSocket/STOMP inventory updates |
| Local Infra | Docker, Docker Compose |
| Mail Sandbox | MailHog |
| Load Test | k6 |
| Testing | JUnit 5, Mockito-ready project structure |

## Core Problem

Flash sale traffic creates a short burst where thousands of users may click purchase at nearly the same time, while stock is limited. If every request directly updates PostgreSQL, the database becomes the bottleneck and oversell risk increases.

This project solves that by moving the first inventory decision to Redis:

1. User sends a purchase request with JWT.
2. Backend validates campaign status and item status.
3. Redis Lua script checks whether the user already bought and whether stock remains.
4. Redis atomically reserves one item and decrements stock.
5. Backend creates an order in `PENDING_PAYMENT` state with a 5-minute payment hold.
6. User confirms mock payment, or a scheduled job cancels expired holds.
7. Payment result is published to RabbitMQ after DB transaction commit.
8. Success changes the order to `CONFIRMED`; failure/timeout changes it to `CANCELLED` and returns stock.
9. WebSocket publishes remaining inventory updates to subscribed clients.

This keeps the database out of the hottest path and makes oversell prevention deterministic at Redis level.

## Architecture

```text
Client / k6
    |
    | JWT request
    v
Spring Boot API
    |
    | validate auth, campaign, item
    v
Redis Lua Inventory Gate
    |
    | reserve success
    v
PostgreSQL Order PENDING_PAYMENT
    |
    | mock payment confirm or timeout
    v
RabbitMQ payment.success / payment.failed
    |
    | consume
    v
CONFIRMED or CANCELLED
    |
    | if cancelled: return stock to Redis + DB
    v
WebSocket Inventory Update
```

## Main Capabilities

- Email/password registration and login.
- JWT access token and refresh token flow.
- Role-based authorization with `USER` and `ADMIN`.
- Admin campaign and flash sale item management.
- Time-based campaign status: `UPCOMING`, `ONGOING`, `ENDED`.
- Redis atomic stock reservation using Lua.
- One-user-one-purchase protection per campaign.
- Mock payment confirmation with idempotent `SUCCESS` / `FAILED` handling.
- Automatic cancellation and stock return for expired payment holds.
- Payment events processed asynchronously through RabbitMQ.
- Realtime remaining stock broadcast through WebSocket/STOMP.
- Campaign thumbnail upload to Cloudinary with CDN optimized image URLs.
- Dockerized local infrastructure for backend, PostgreSQL, Redis, RabbitMQ, and MailHog.
- k6 script for high-concurrency flash sale simulation.

## Infrastructure

`docker-compose.yml` starts:

| Service | Purpose |
|---|---|
| `backend` | Spring Boot API |
| `postgres` | Main relational database |
| `redis` | Atomic stock gate and fast inventory state |
| `rabbitmq` | Async order processing queue |
| `mailhog` | Local email sandbox for future notification flow |

Run:

```powershell
docker compose up --build
```

Useful local URLs:

| Service | URL |
|---|---|
| Backend API | `http://localhost:8080` |
| RabbitMQ Management | `http://localhost:15672` |
| MailHog | `http://localhost:8025` |

RabbitMQ default account:

```text
guest / guest
```

## High-Concurrency Design

The purchase endpoint is designed for burst traffic. Instead of locking a PostgreSQL row for every click, the first stock decision is handled by Redis.

Redis keeps:

```text
flashsale:campaign:{campaignId}:item:{itemId}:stock
flashsale:campaign:{campaignId}:buyers
```

The Lua script performs these operations atomically:

- reject if buyer already exists in the campaign buyer set;
- initialize stock from DB snapshot when key is missing;
- reject when stock is `0`;
- decrement stock;
- add buyer to buyer set;
- return remaining stock.

Because the check and decrement happen in one Redis command, concurrent requests cannot oversell the item.

## Mock Payment Flow

```text
Mua hang
  |
  v
Redis tru kho atomic
  |
  v
Order PENDING_PAYMENT + paymentExpiresAt
  |
  +-- Mock confirm SUCCESS --> Order CONFIRMED --> RabbitMQ payment.success --> MailHog confirmation
  |
  +-- Mock confirm FAILED  --> Order CANCELLED --> RabbitMQ payment.failed  --> return stock + failure email
  |
  +-- Timeout scheduler    --> Order CANCELLED --> RabbitMQ payment.failed  --> return stock + failure email
```

The mock payment endpoint is idempotent at order level. If the order is already `CONFIRMED` or `CANCELLED`, repeated calls return the existing result instead of processing inventory again.

## Campaign Thumbnail Storage

Campaign thumbnails are uploaded to Cloudinary instead of being stored on the backend server. The backend validates file type and size, uploads to a campaign-specific Cloudinary folder, stores `thumbnail_url` and `thumbnail_public_id`, and returns an optimized CDN URL using Cloudinary transformations:

```text
w_400,h_300,c_fill,q_auto,f_auto
```

When an admin replaces a thumbnail, the backend deletes the previous Cloudinary asset by `public_id` to avoid unused media buildup.

## Load Test Target

The included k6 scenario is intended to simulate a flash sale spike:

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e USER_TOKENS="token1,token2,token3" `
  -e CAMPAIGN_ID=1 `
  -e ITEM_ID=1 `
  -e VUS=1000 `
  -e ITERATIONS=10000 `
  load-test/purchase-10000.js
```

Expected behavior when stock is `100` and `10,000` purchase attempts arrive:

| Metric | Expected Result |
|---|---|
| Successful reservations | At most `100` pending/confirmed orders |
| Oversell | `0` |
| Redis stock | Ends at `0` |
| Duplicate user purchase | Rejected |
| Excess requests | Return conflict such as `OUT_OF_STOCK` or `ALREADY_PURCHASED` |
| Database pressure | Reduced because Redis rejects most losing requests before final order work |

Actual throughput and latency numbers should be filled in after running k6 in the target machine or IntelliJ-run environment.

Suggested metrics to capture:

| Metric | Source |
|---|---|
| requests per second | k6 summary |
| p50 / p95 / p99 latency | k6 summary |
| success count | k6 checks plus PostgreSQL order count |
| conflict count | k6 status distribution |
| oversell count | compare successful orders against item stock |
| Redis final stock | Redis CLI |
| RabbitMQ queue depth | RabbitMQ Management UI |

## Verification Queries

Redis:

```bash
GET flashsale:campaign:{campaignId}:item:{itemId}:stock
SCARD flashsale:campaign:{campaignId}:buyers
```

PostgreSQL:

```sql
select status, count(*)
from orders
where campaign_id = :campaignId
group by status;
```

```sql
select id, total_quantity, remaining_quantity
from flash_sale_items
where id = :itemId;
```

## Project Highlights For CV

- Built a Java 21 + Spring Boot flash sale backend designed for high-concurrency purchase spikes.
- Implemented JWT authentication with access/refresh tokens and role-based authorization using Spring Security.
- Prevented overselling by using Redis Lua scripting for atomic stock reservation under concurrent traffic.
- Reduced database pressure by rejecting losing purchase requests at Redis before expensive order processing.
- Added RabbitMQ-based asynchronous order finalization with publish-after-commit semantics to avoid race conditions.
- Implemented event-driven mock payment flow with idempotent confirmation, timeout cancellation, and automatic stock return.
- Integrated Cloudinary for campaign thumbnail upload, CDN delivery, optimized transformations, and old image cleanup.
- Added WebSocket/STOMP inventory updates for realtime remaining-stock visibility.
- Dockerized the backend stack with PostgreSQL, Redis, RabbitMQ, and MailHog for reproducible local runs.
- Added k6 load-test script for simulating 10,000 flash sale purchase attempts and validating zero-oversell behavior.

## Notes

- Maven does not need to be available globally on the machine if the project is built and run inside IntelliJ with its configured JDK/Maven environment.
- If running outside IntelliJ, use JDK 21 and the Maven wrapper: `.\mvnw.cmd test`.
- The project currently focuses on backend and infrastructure. Frontend is intentionally out of scope.
