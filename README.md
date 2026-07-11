# Flash Sale Service 🚀

A high-performance, multi-tenant **Flash Sale** backend system built with Spring Boot, designed to handle thousands of concurrent checkout requests with atomic inventory management, real-time notifications, and robust race condition prevention.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Quick Start with Docker](#quick-start-with-docker)
  - [Configuration](#configuration)
- [API Reference](#api-reference)
  - [Authentication](#authentication)
  - [Campaign Management](#campaign-management)
  - [Product Management](#product-management)
  - [Checkout & Order](#checkout--order)
  - [Public API](#public-api)
- [Core Design & Key Concepts](#core-design--key-concepts)
  - [Multi-Tenancy](#multi-tenancy)
  - [Atomic Inventory Deduction (Lua + Redis)](#atomic-inventory-deduction-lua--redis)
  - [Async Order Processing (RabbitMQ)](#async-order-processing-rabbitmq)
  - [Payment Timeout & Auto-Cancellation](#payment-timeout--auto-cancellation)
  - [Idempotency](#idempotency)
  - [Rate Limiting (Bucket4j)](#rate-limiting-bucket4j)
  - [Real-Time Notifications (WebSocket)](#real-time-notifications-websocket)
- [Load Testing](#load-testing)
- [Security](#security)
- [Error Handling](#error-handling)

---

## Overview

**Flash Sale Service** is a SaaS-ready backend that enables businesses to create and manage flash sale campaigns. It provides:

- **Multi-tenant architecture** — each company (tenant) gets isolated data and API keys.
- **High-concurrency checkout** — uses Redis + Lua scripting for atomic stock deduction, preventing overselling.
- **Async order processing** — RabbitMQ decouples the hot checkout path from database writes.
- **Payment timeout handling** — Redis key expiration events trigger automatic order cancellation and stock restoration.
- **Real-time updates** — WebSocket pushes stock changes and order status to clients.
- **Rate limiting** — Bucket4j with Redis backend protects against abuse.

---

### Data Flow: Checkout Process

1. **Client** sends `POST /api/v1/orders/checkout` with `productId`, `quantity`, `idempotencyKey`.
2. **Rate Limit Filter** checks if the caller hasn't exceeded the limit (5 req/s).
3. **Auth Filter** validates JWT or API Key, extracts `tenantId` and `userId`.
4. **CheckoutService** loads a **Lua script** that atomically:
   - Checks if the user already bought this product (anti-cheat).
   - Checks available stock in Redis.
   - Decrements stock and marks the user as "bought" with a 5-minute TTL.
5. If successful, a message is pushed to **RabbitMQ** `order.create.queue`.
6. **OrderMessageListener** consumes the message and calls `OrderService.createOrder()`:
   - Saves the order to **PostgreSQL** with status `RESERVED`.
   - Sets a Redis key `order_timeout:{orderId}` with 5-minute TTL.
   - Sends a **WebSocket** notification to the user.
7. If the user pays within 5 minutes (mock: `POST /orders/{id}/success`), status changes to `PAID` and the Redis timeout key is deleted.
8. If the Redis key expires, **RedisKeyExpirationListener** triggers `cancelExpiredOrder()`, which:
   - Sets order status to `CANCELLED`.
   - Restores stock in Redis.
   - Deletes the user's "bought" flag so they can retry.

---

## Tech Stack

| Category              | Technology                                      |
|-----------------------|-------------------------------------------------|
| **Language**          | Java 17                                         |
| **Framework**         | Spring Boot 3.5.16                              |
| **Database**          | PostgreSQL 15 (via Spring Data JPA + Hibernate) |
| **Cache**             | Redis 7 (via Spring Data Redis)                 |
| **Message Queue**     | RabbitMQ 3 (via Spring AMQP)                    |
| **Real-time**         | WebSocket (STOMP over SockJS)                   |
| **Auth**              | JWT (jjwt 0.13) + API Key (SHA-256 hashed)      |
| **Rate Limiting**     | Bucket4j 8.1 + Lettuce (Redis backend)          |
| **Build Tool**        | Maven                                           |
| **Containerization**  | Docker & Docker Compose                         |
| **Load Testing**      | k6                                              |

---

## Features

### ✅ Multi-Tenant SaaS
- Each company registers and gets isolated data (`tenant_id` on all entities).
- Automatic API Key generation (`pk_live_*` / `sk_live_*`) on registration.
- JWT authentication for admin/staff users.
- API Key authentication for public/landing page access.

### ✅ Flash Sale Campaign Management
- Create campaigns with start/end times.
- Add products with SKU, price, and stock quantity.
- Campaign status lifecycle: `PENDING` → `ACTIVE` → `COMPLETED`.
- Sync inventory to Redis cache for high-speed checkout.

### ✅ High-Concurrency Checkout
- **Atomic stock deduction** via Redis Lua script — no race conditions.
- **Anti-cheat**: prevents users from buying the same product twice.
- **Idempotency**: each request carries a unique `idempotencyKey` to prevent duplicate orders.
- **Async processing**: RabbitMQ decouples checkout from database writes.

### ✅ Payment Timeout Handling
- Orders start as `RESERVED` with a 5-minute payment window.
- Redis key expiration (`__keyevent@0__:expired`) triggers automatic cancellation.
- On cancellation: stock is restored, user's buy flag is cleared.

### ✅ Rate Limiting
- Bucket4j with Redis backend (distributed rate limiting).
- Default: 5 requests per second per caller (configurable).
- Returns `429 Too Many Requests` with retry-after header.

### ✅ Real-Time Notifications
- WebSocket (STOMP over SockJS) for real-time updates.
- Broadcast stock changes to all clients (`/topic/campaigns/{id}/stock`).
- Private order status notifications (`/queue/orders`).

### ✅ Security
- JWT-based authentication for admin APIs.
- API Key (SHA-256 hashed) for public APIs.
- AES-256 encryption for sensitive data.
- BCrypt password hashing.
- CORS/CSRF disabled (stateless API).

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Docker** & **Docker Compose** (for PostgreSQL, Redis, RabbitMQ)
- **Maven** (or use the included Maven wrapper `mvnw`)

### Quick Start with Docker

1. **Clone the repository**

```bash
git clone https://github.com/lamtriphuc/flash-sale-service.git
cd flash-sale-service
```

2. **Start infrastructure services**

```bash
docker compose up -d
```

This starts:
- PostgreSQL 15 on port `5432`
- Redis 7 on port `6379` (with keyspace notifications enabled)
- RabbitMQ 3 on port `5672` (AMQP) and `15672` (Management UI)

3. **Run the application**

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

### Configuration

Key configuration in `src/main/resources/application.yaml`:

| Property              | Description                          | Default Value                                    |
|-----------------------|--------------------------------------|--------------------------------------------------|
| `spring.datasource.*` | PostgreSQL connection                 | `localhost:5432/flash_sale`                      |
| `spring.data.redis.*` | Redis connection                      | `localhost:6379`                                 |
| `spring.rabbitmq.*`   | RabbitMQ connection                   | `localhost:5672` (admin/admin)                   |
| `jwt.secret`          | JWT signing key (≥32 chars)           | `MySuperSecretKeyForJwtAuthentication123456789!@#` |
| `jwt.expirationMs`    | JWT expiration (ms)                   | `86400000` (24 hours)                            |
| `crypto.aes.secret`   | AES encryption key (32 bytes)         | `MySuperSecretKeyForAes2561234567`               |
| `server.port`         | Application port                      | `8080`                                           |

---

## API Reference

### Authentication

All admin APIs require a JWT token in the `Authorization: Bearer <token>` header.

#### Register a new tenant

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "companyName": "My Shop",
  "email": "admin@myshop.com",
  "password": "securePassword123"
}
```

**Response** (201 Created):
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "companyName": "My Shop",
    "role": "ADMIN",
    "publicKey": "pk_live_<random>",
    "secretKey": "sk_live_<random>"
  }
}
```

> **Note:** Save the `publicKey` and `secretKey` — they are shown only once. The `publicKey` is used for public API access.

#### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@myshop.com",
  "password": "securePassword123"
}
```

**Response** (200 OK):
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "companyName": "My Shop",
    "role": "ADMIN"
  }
}
```

---

### Campaign Management

All endpoints require `Authorization: Bearer <token>`.

#### Create a campaign

```http
POST /api/v1/campaigns
Content-Type: application/json

{
  "name": "Summer Flash Sale 2026",
  "startTime": "2026-07-15T00:00:00",
  "endTime": "2026-07-16T00:00:00"
}
```

#### Get all campaigns

```http
GET /api/v1/campaigns
```

#### Get campaign by ID

```http
GET /api/v1/campaigns/{campaignId}
```

---

### Product Management

#### Add product to campaign

```http
POST /api/v1/campaigns/{campaignId}/products
Content-Type: application/json

{
  "sku": "IPHONE-15-PRO",
  "name": "iPhone 15 Pro 256GB",
  "price": 29990000,
  "quantity": 100
}
```

#### Get products by campaign

```http
GET /api/v1/campaigns/{campaignId}/products
```

#### Sync inventory to Redis

Before the flash sale starts, sync product stock from PostgreSQL to Redis:

```http
POST /api/v1/campaigns/{campaignId}/sync-inventory
```

---

### Checkout & Order

#### Checkout (place an order)

```http
POST /api/v1/orders/checkout
Content-Type: application/json
Authorization: Bearer <token>

{
  "productId": "uuid-of-product",
  "quantity": 1,
  "idempotencyKey": "unique-key-per-request"
}
```

**Response** (202 Accepted):
```json
{
  "code": 200,
  "message": "Thành công",
  "data": null
}
```

> The actual order is created asynchronously. Listen on WebSocket `/user/queue/orders` for the result.

#### Mock payment success

```http
POST /api/v1/orders/{orderId}/success
```

Changes order status from `RESERVED` to `PAID` and removes the Redis timeout alarm.

#### Mock payment cancel

```http
POST /api/v1/orders/{orderId}/cancel
```

Cancels the order and restores stock in Redis.

---

### Public API

Public endpoints use **API Key authentication** via the `X-API-Key` header.

#### Get campaign details for landing page

```http
GET /api/v1/public/campaigns/{campaignId}
X-API-Key: pk_live_<your-public-key>
```

**Response**:
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "campaign": { ... },
    "products": [ ... ]
  }
}
```

---

## Core Design & Key Concepts

### Multi-Tenancy

Every entity (Campaign, Product, Order) has a `tenant_id` column. All queries filter by the current tenant extracted from the JWT or API Key. This ensures complete data isolation between companies.

### Atomic Inventory Deduction (Lua + Redis)

The heart of the flash sale is the Lua script (`checkout.lua`):

```lua
-- KEYS[1]: stock_key (stock:{tenantId}:{productId})
-- KEYS[2]: user_bought_key (bought:{tenantId}:{productId}:{userId})
-- ARGV[1]: quantity

-- 1. Anti-Cheat: Check if user already bought
if redis.call("EXISTS", user_bought_key) == 1 then
    return -1  -- Already bought
end

-- 2. Check stock
local current_stock = tonumber(redis.call("GET", stock_key))
if current_stock and current_stock >= quantity then
    redis.call("DECRBY", stock_key, quantity)
    redis.call("SETEX", user_bought_key, 300, "1")  -- 5 min TTL
    return 1  -- Success
else
    return 0  -- Out of stock
end
```

**Why Lua?** Redis guarantees atomic execution of Lua scripts, eliminating race conditions without distributed locks.

### Async Order Processing (RabbitMQ)

The checkout path is kept lightweight:
1. **Synchronous**: Validate auth, run Lua script (milliseconds).
2. **Async**: Push message to RabbitMQ → worker saves to PostgreSQL.

This prevents database connection pool exhaustion during traffic spikes.

### Payment Timeout & Auto-Cancellation

- When an order is created, a Redis key `order_timeout:{orderId}` is set with a 5-minute TTL.
- Redis is configured with `--notify-keyspace-events Ex` to emit expiration events.
- `RedisKeyExpirationListener` catches these events and calls `cancelExpiredOrder()`.
- On cancellation: order status → `CANCELLED`, stock restored, user buy flag cleared.

### Idempotency

Each checkout request includes a unique `idempotencyKey`. The `Order` table has a unique constraint on this key, preventing duplicate order creation even if the client retries or RabbitMQ delivers the message twice.

### Rate Limiting (Bucket4j)

- **Algorithm**: Token Bucket (5 tokens, refills 5 every second).
- **Backend**: Redis (distributed, survives restarts).
- **Scope**: Per caller (identified by API Key hash, JWT token hash, or IP address).
- **Response**: `429 Too Many Requests` with `X-Rate-Limit-Retry-After-Seconds` header.

### Real-Time Notifications (WebSocket)

- **Endpoint**: `ws://localhost:8080/ws-endpoint` (STOMP over SockJS).
- **Broadcast channel**: `/topic/campaigns/{campaignId}/stock` — stock updates for all clients.
- **Private channel**: `/user/queue/orders` — order confirmation for individual users.
- **Auth**: JWT token sent in the STOMP `CONNECT` frame's `Authorization` header.

---

## Load Testing

A k6 load test script is provided at `load-tests/flash_sale_test.js`.

### Run the test

1. **Install k6**: https://k6.io/docs/getting-started/installation/
2. **Update the script** with your product ID and API key.
3. **Run**:

```bash
k6 run load-tests/flash_sale_test.js
```

### Test Scenario

- **1000 concurrent virtual users** (VUs) hitting the checkout endpoint simultaneously.
- **Duration**: 10 seconds.
- **Thresholds**:
  - 95% of requests complete in under 500ms.
  - Less than 1% failure rate (5xx errors).

### Expected Results

| Status Code | Meaning                    | Expected % |
|-------------|----------------------------|------------|
| 202         | Accepted (queued)          | ~10-20%    |
| 409         | Out of stock / Already bought | ~80-90% |
| 429         | Rate limited               | ~0-5%      |
| 500         | Server error               | <1%        |

---

## Security

| Layer              | Mechanism                                    |
|--------------------|----------------------------------------------|
| **Authentication** | JWT (admin/staff) + API Key (public clients) |
| **Password Storage** | BCrypt                                      |
| **API Key Storage**  | SHA-256 hash (never store raw keys)         |
| **Data Encryption**  | AES-256 for sensitive profile data          |
| **Rate Limiting**    | Bucket4j (5 req/s per caller)               |
| **Idempotency**      | Unique constraint on `idempotency_key`       |
| **Anti-Cheat**       | Redis flag prevents duplicate purchases      |

---

## Error Handling

All errors follow a consistent JSON format:

```json
{
  "code": 409,
  "message": "Sản phẩm đã hết hàng"
}
```

### Common Error Codes

| Code | HTTP Status | Message                        | When                          |
|------|-------------|--------------------------------|-------------------------------|
| 200  | 200         | Thành công                     | Success                       |
| 400  | 400         | Dữ liệu đầu vào không hợp lệ   | Validation error              |
| 401  | 401         | Chưa xác thực danh tính        | Missing/invalid token         |
| 403  | 403         | Bạn không có quyền truy cập    | Forbidden                     |
| 404  | 404         | Không tìm thấy tài nguyên      | Resource not found            |
| 409  | 409         | Sản phẩm đã hết hàng           | Out of stock                  |
| 409  | 409         | Bạn đã tham gia mua sản phẩm này rồi | Duplicate purchase     |
| 429  | 429         | Bạn đang thao tác quá nhanh    | Rate limit exceeded           |
| 500  | 500         | Lỗi hệ thống nội bộ            | Internal server error         |

---

## 📄 License

This project is licensed under the MIT License.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add some amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

---

## 📧 Contact

Project Link: [https://github.com/lamtriphuc/flash-sale-service](https://github.com/lamtriphuc/flash-sale-service)