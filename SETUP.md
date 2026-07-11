# Hướng dẫn Setup dự án

## 1. Clone dự án

```bash
git clone <repository-url>
cd flash-sale-service
```

## 2. Cấu hình Secrets

### a. Copy file template

```bash
# Windows
copy application.yaml.example application.yaml
copy docker-compose.override.yml.example docker-compose.override.yml

# Linux/Mac
cp application.yaml.example application.yaml
cp docker-compose.override.yml.example docker-compose.override.yml
```

### b. Điền thông tin vào `application.yaml`

```yaml
spring:
  datasource:
    password: YOUR_DB_PASSWORD  # Đổi mật khẩu database

  rabbitmq:
    password: YOUR_RABBITMQ_PASSWORD  # Đổi mật khẩu RabbitMQ

jwt:
  secret: YOUR_JWT_SECRET_KEY_HERE  # Đổi secret key (ít nhất 256 bits)

crypto:
  aes:
    secret: YOUR_AES_SECRET_KEY_HERE  # Đổi AES secret key
```

### c. Điền thông tin vào `docker-compose.override.yml`

```yaml
services:
  postgres:
    environment:
      POSTGRES_PASSWORD: YOUR_POSTGRES_PASSWORD  # Phải khớp với application.yaml

  rabbitmq:
    environment:
      RABBITMQ_DEFAULT_PASS: YOUR_RABBITMQ_PASSWORD  # Phải khớp với application.yaml
```

## 3. Khởi chạy Infrastructure

```bash
# Start PostgreSQL, Redis, RabbitMQ
docker-compose up -d

# Verify services
docker-compose ps
```

## 4. Build và chạy ứng dụng

```bash
# Windows
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw clean compile
./mvnw spring-boot:run
```

## 5. Test ứng dụng

### a. E2E Test

```bash
# Windows
e2e-test.bat

# Linux/Mac
bash e2e-test.sh
```

### b. Load Test (cần cài Docker)

```bash
docker run --rm -i grafana/k6 run - < load-tests/flash_sale_test.js
```

## 6. Truy cập các dịch vụ

- **API:** http://localhost:8080
- **RabbitMQ Management:** http://localhost:15672 (admin/admin)
- **PostgreSQL:** localhost:5432

## Lưu ý bảo mật

- **KHÔNG** commit file `application.yaml` lên Git
- **KHÔNG** commit file `docker-compose.override.yml` lên Git
- **KHÔNG** commit file `.env` hoặc `.env.local` lên Git
- Sử dụng strong passwords cho production
- Rotate secrets định kỳ