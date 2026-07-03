# Prompt cho AI Agent: Xây dựng hệ thống Flash Sale (đơn tenant, sẵn sàng mở rộng)

Copy toàn bộ nội dung dưới đây và đưa cho Agent (Claude Code, Cursor, v.v.) để bắt đầu dự án.

---

## VAI TRÒ

Bạn là một Senior Backend/Fullstack Engineer, sẽ giúp tôi xây dựng một dự án cá nhân để chứng minh năng lực kỹ thuật khi apply vị trí Java Backend Developer. Mục tiêu không chỉ là "chạy được" mà là thể hiện tư duy hệ thống: xử lý concurrency, caching, async processing, và có số liệu load test thực tế.

## BỐI CẢNH DỰ ÁN

Xây dựng **"FlashDeal" — hệ thống Flash Sale**: cho phép admin tạo các campaign giảm giá có giới hạn số lượng và thời gian, người dùng đăng ký/đăng nhập để tham gia mua hàng khi campaign mở, hệ thống phải xử lý đúng inventory dưới tải cao (nhiều người mua cùng lúc, số lượng có hạn).

Thiết kế **đơn tenant** (1 hệ thống, không multi-tenant thật) nhưng schema và API đặt sẵn `tenant_id` ở các bảng liên quan để thể hiện tư duy sẵn sàng mở rộng sau này — không cần implement logic multi-tenant thật, chỉ cần có field và comment giải thích lý do.

## TECH STACK

**Backend:**
- Java 21 + Spring Boot 3.x
- Spring Security + JWT (access token + refresh token)
- Spring Data JPA + Hibernate
- PostgreSQL (database chính)
- Redis (cache + atomic inventory decrement bằng Lua script hoặc `DECR`)
- RabbitMQ (queue xử lý order bất đồng bộ sau khi giữ chỗ thành công)
- Flyway (database migration, versioned)
- MapStruct (DTO mapping)
- Bucket4j hoặc Resilience4j (rate limiting theo user/IP)
- JUnit 5 + Mockito (unit test)
- Testcontainers (integration test với Postgres + Redis thật trong container)
- Springdoc OpenAPI (swagger UI cho API docs)

**Frontend:**
- ReactJS + JavaScript
- Zustand cho state management (auth state, cart tạm)
- Tailwind CSS
- Countdown timer component cho campaign

**Infra / DevOps:**
- Docker + Docker Compose (Postgres, Redis, RabbitMQ, Backend, Frontend chạy chung 1 lệnh `docker compose up`)
- GitHub Actions: pipeline build + test tự động khi push
- k6 (load testing, viết script test kịch bản flash sale thật)

**Monitoring (nếu có thời gian, optional):**
- Spring Boot Actuator + Micrometer + Prometheus + Grafana (theo dõi request rate, latency, error rate trong lúc load test)

## YÊU CẦU CHỨC NĂNG

### 1. Authentication & Authorization
- Đăng ký/đăng nhập bằng email + password
- JWT access token (thời gian sống ngắn) + refresh token (lưu ở DB hoặc Redis, có thể revoke)
- 2 role: `USER` và `ADMIN`
- Endpoint đổi mật khẩu, lấy thông tin profile

### 2. Quản lý Campaign (Admin)
- CRUD campaign: tên, mô tả, sản phẩm liên kết, số lượng giới hạn (`total_stock`), thời gian bắt đầu/kết thúc, giá flash sale
- Trạng thái campaign: `UPCOMING`, `ONGOING`, `ENDED` (tính tự động dựa trên thời gian, không lưu cứng)
- Admin xem được thống kê real-time: đã bán bao nhiêu, còn lại bao nhiêu (đọc từ Redis, không query DB trực tiếp để tránh tải)

### 3. Trải nghiệm mua hàng (User) — đây là phần trọng tâm
- Trang xem danh sách campaign sắp diễn ra + đang diễn ra, có countdown
- Khi campaign `ONGOING`, user bấm "Mua ngay":
  - Hệ thống kiểm tra và trừ tồn kho **atomic** bằng Redis (Lua script: kiểm tra + trừ trong 1 lệnh duy nhất, tránh race condition)
  - Nếu còn hàng → giữ chỗ (reserve) → đẩy message vào RabbitMQ để xử lý tạo order chính thức (ghi vào Postgres, gửi email xác nhận)
  - Nếu hết hàng → trả lỗi rõ ràng ngay lập tức, không để user chờ
- Giới hạn: mỗi user chỉ được mua tối đa N sản phẩm/campaign (chống 1 người quét hết hàng)
- Rate limiting theo user_id và IP cho endpoint mua hàng (chống bot spam click)

### 4. Order & Notification
- Consumer lắng nghe queue RabbitMQ → tạo order trong Postgres → gọi service gửi email xác nhận (dùng JavaMailSender + MailHog để test local, không cần gửi email thật)
- User xem lịch sử đơn hàng của mình

### 5. So sánh 2 cách xử lý concurrency (mục đích viết blog/demo phỏng vấn)
Implement **cả 2 cách** xử lý trừ tồn kho, có thể bật/tắt bằng config hoặc endpoint riêng để so sánh:
- **Cách A**: Pessimistic locking ở DB (`SELECT ... FOR UPDATE`)
- **Cách B**: Redis atomic decrement (Lua script)

Sau đó load test cả 2 cách, ghi lại số liệu: throughput (request/giây), latency (p50/p95/p99), tỷ lệ lỗi/oversell (nếu có), để so sánh và viết thành báo cáo.

## YÊU CẦU PHI CHỨC NĂNG

- Database schema chuẩn hóa, có index hợp lý (đặc biệt cột dùng để filter/join nhiều)
- Toàn bộ exception được xử lý tập trung qua `@ControllerAdvice`, trả về error response nhất quán (có error code, message, timestamp)
- API versioning (`/api/v1/...`)
- Validate input đầy đủ (Bean Validation)
- Viết unit test cho phần logic nghiệp vụ quan trọng nhất (xử lý inventory), coverage tối thiểu phần này gần 100%
- Viết ít nhất 2-3 integration test bằng Testcontainers, đặc biệt test race condition (nhiều thread cùng gọi API mua hàng, verify không bị oversell)
- README rõ ràng: kiến trúc hệ thống (vẽ sơ đồ ASCII hoặc mô tả), hướng dẫn chạy `docker compose up`, kết quả load test kèm nhận xét

## KẾ HOẠCH TRIỂN KHAI (đề xuất thứ tự cho Agent)

Thực hiện theo từng giai đoạn, sau mỗi giai đoạn dừng lại để tôi review trước khi làm tiếp:

1. **Giai đoạn 1**: Thiết kế database schema (ERD) + viết migration Flyway. Trình bày ERD trước khi code.
2. **Giai đoạn 2**: Setup project skeleton (Spring Boot + cấu trúc package chuẩn layered architecture) + Docker Compose cho toàn bộ hạ tầng (Postgres, Redis, RabbitMQ, MailHog).
3. **Giai đoạn 3**: Auth module (đăng ký, đăng nhập, JWT, refresh token).
4. **Giai đoạn 4**: Campaign module (CRUD cho admin, API xem danh sách cho user).
5. **Giai đoạn 5**: Core mua hàng — implement cách A (DB lock) trước, viết test race condition để verify đúng.
6. **Giai đoạn 6**: Implement cách B (Redis atomic) song song, có thể so sánh.
7. **Giai đoạn 7**: RabbitMQ consumer tạo order + gửi email.
8. **Giai đoạn 8**: Rate limiting.
9. **Giai đoạn 9**: Frontend (React) — trang danh sách campaign, countdown, trang mua hàng, trang admin.
10. **Giai đoạn 10**: Viết script k6 load test, chạy test, tổng hợp số liệu vào README.
11. **Giai đoạn 11**: GitHub Actions CI pipeline.

## YÊU CẦU KHI LÀM VIỆC

- Giải thích ngắn gọn quyết định kỹ thuật quan trọng (VD: vì sao chọn pessimistic lock thay vì optimistic ở chỗ này) để tôi hiểu và có thể trình bày lại khi phỏng vấn.
- Code phải sạch, có comment ở những đoạn logic phức tạp (đặc biệt phần xử lý concurrency).
- Không cần làm frontend quá cầu kỳ về UI, ưu tiên đúng chức năng và trải nghiệm mượt (loading state, error state rõ ràng).
- Sau khi hoàn thành, viết một bản tóm tắt các điểm kỹ thuật nổi bật của dự án theo dạng gạch đầu dòng, để tôi dùng làm gạch đầu dòng trình bày trong CV/phỏng vấn.

Hãy bắt đầu với **Giai đoạn 1**: đề xuất ERD (database schema) chi tiết, kèm giải thích cho từng bảng và các field `tenant_id`/`campaign_id` sẽ đặt ở đâu để sẵn sàng mở rộng.
