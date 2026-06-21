# Đặc tả nghiệp vụ — Flash Sale BaaS

## 1. Tổng quan

Xây dựng một nền tảng **Backend-as-a-Service (BaaS)** thu nhỏ, cho phép các
**tenant** (doanh nghiệp/đối tác tích hợp) tạo và vận hành các chiến dịch
**Flash Sale** thông qua REST API, mà không cần tự xây dựng logic chống
oversell, chống mua trùng từ đầu.

Phạm vi hiện tại: **chỉ Flash Sale** (không bao gồm Lucky Draw).

## 2. Các vai trò (Actors)

| Vai trò | Mô tả | Tương tác với hệ thống |
|---|---|---|
| **Tenant Admin** | Nhân viên/quản trị viên thuộc về 1 Tenant | Đăng nhập bằng username/password vào hệ thống quản trị (control plane) để tạo campaign, quản lý sản phẩm, xem báo cáo, quản lý API Key |
| **Tenant** (hệ thống) | Hệ thống backend của doanh nghiệp tích hợp BaaS | Gọi REST API bằng API Key (không qua đăng nhập) để vận hành campaign theo thời gian thực — đây là kênh chính dùng cho luồng mua hàng tốc độ cao |
| **End-user** | Người dùng cuối tham gia mua flash sale | Không tương tác trực tiếp với BaaS — đi qua app/web của Tenant. Định danh bằng `userId` (string) do Tenant truyền vào, BaaS không lưu thông tin cá nhân |
| **BaaS System** | Hệ thống đang xây dựng | Xử lý request, đảm bảo tính đúng đắn của tồn kho và lượt mua |

> **Lưu ý quan trọng:** BaaS không có khái niệm "đăng ký/đăng nhập" cho
> end-user. End-user được xác thực bởi hệ thống của Tenant; BaaS chỉ nhận
> một `userId` làm định danh duy nhất trong phạm vi 1 tenant.
>
> Có **2 cơ chế xác thực riêng biệt, phục vụ 2 mục đích khác nhau**:
> - **Username/password (JWT)** — dành cho **Tenant Admin** thao tác qua
>   dashboard/control plane (tạo campaign, quản lý item, xem báo cáo)
> - **API Key** — dành cho **hệ thống backend của Tenant** gọi trực tiếp
>   vào luồng nghiệp vụ tốc độ cao (đặc biệt là endpoint mua hàng)
>
> Hai cơ chế không thay thế nhau: Tenant Admin đăng nhập để *cấu hình*
> chiến dịch, còn API Key dùng để *vận hành* chiến dịch ở quy mô lớn.

## 3. Phạm vi nghiệp vụ (Scope)

### 3.1. Trong phạm vi (In scope)
- Quản lý Tenant và API Key
- **Tenant Admin**: đăng ký/đăng nhập (username + password), quản lý
  bằng JWT, thao tác trên dữ liệu của đúng Tenant mình thuộc về
- Quản lý vòng đời Campaign (tạo, kích hoạt, kết thúc)
- Quản lý sản phẩm flash sale (FlashSaleItem) — nhiều SKU trong 1 campaign
- Xử lý request mua hàng với đảm bảo:
  - Không bán vượt quá tồn kho (chống oversell)
  - Mỗi user chỉ được mua **1 lần duy nhất** trong 1 campaign (không phân
    biệt theo SKU — đã mua 1 SKU bất kỳ trong campaign thì không được mua
    thêm SKU khác trong cùng campaign đó)
- Tra cứu kết quả/lịch sử mua hàng

### 3.2. Ngoài phạm vi (Out of scope — giai đoạn này)
- Lucky Draw / quay số trúng thưởng
- Thanh toán thực tế (payment gateway)
- Giao hàng, vận chuyển
- Quản lý profile end-user
- Dashboard quản trị có giao diện đẹp (chỉ cần API + Swagger)
- Hàng đợi (queue) xử lý bất đồng bộ, WebSocket real-time
- Redis (giai đoạn đầu dùng giải pháp chống race condition ở tầng DB)

## 4. Mô hình nghiệp vụ (Domain Model)

> **Ghi chú kỹ thuật:** Id dùng `Long` (auto-increment) thay vì `UUID`, và
> giá tiền dùng `Long` (đơn vị VNĐ nguyên, không có phần thập phân) thay vì
> `Decimal` — đây là lựa chọn triển khai hợp lệ, không ảnh hưởng đến quy
> tắc nghiệp vụ mô tả bên dưới.

### 4.1. Tenant
Đại diện cho 1 khách hàng B2B sử dụng BaaS.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh tenant |
| code | String | Mã định danh dễ đọc, duy nhất (vd: `SHOP_A`) |
| name | String | Tên doanh nghiệp |
| contactEmail | String | Email liên hệ |
| active | Boolean | `true` = đang hoạt động, `false` = bị tạm ngưng |
| createdAt | Timestamp | Ngày tạo |

**Quy tắc nghiệp vụ:**
- Tenant có `active = false` không thể gọi bất kỳ API nghiệp vụ nào (trả
  lỗi 403)
- `code` là duy nhất trên toàn hệ thống

### 4.2. ApiKey
Khóa xác thực gắn liền với 1 Tenant.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh |
| tenant | Tenant | Tenant sở hữu (quan hệ N-1) |
| keyValue | String | Giá trị API key, duy nhất |
| name | String | Tên gợi nhớ, vd: `Production Key`, `Test Key` |
| active | Boolean | `true` = đang dùng được, `false` = đã thu hồi |
| expiredAt | Timestamp (nullable) | Thời điểm hết hạn, `null` = không hết hạn |
| createdAt | Timestamp | Ngày tạo |

**Quy tắc nghiệp vụ:**
- 1 Tenant có thể có nhiều API Key (hỗ trợ xoay vòng key, tách biệt
  môi trường test/production)
- Mọi request nghiệp vụ (trừ tạo Tenant) bắt buộc có header `X-API-Key`
  hợp lệ, đang `active = true`, và chưa quá `expiredAt` (nếu có)
- API Key không hợp lệ/đã thu hồi/hết hạn → từ chối request (401)

### 4.3. User (Tenant Admin)
Tài khoản quản trị, thuộc về 1 Tenant, dùng để đăng nhập vào hệ thống
quản trị (control plane) — **khác hoàn toàn** với end-user mua hàng.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh |
| tenant | Tenant | Tenant mà tài khoản này thuộc về (quan hệ N-1) |
| username | String | Tên đăng nhập, duy nhất trong phạm vi 1 tenant |
| passwordHash | String | Mật khẩu đã băm (bcrypt), không bao giờ trả ra API |
| fullName | String | Họ tên hiển thị |
| role | Enum | `OWNER`, `STAFF` (xem ghi chú phân quyền) |
| active | Boolean | `true` = đang hoạt động, `false` = bị khóa |
| createdAt | Timestamp | Ngày tạo |

**Quy tắc nghiệp vụ:**
- Cặp `(tenantId, username)` là duy nhất — 2 tenant khác nhau được phép
  trùng `username`
- Đăng nhập thành công → trả về JWT chứa `userId`, `tenantId`, `role`,
  dùng để xác thực các API thuộc control plane (`/api/v1/admin/**`)
- Phân quyền tối giản cho giai đoạn này: `OWNER` được thao tác mọi thứ
  (bao gồm tạo/thu hồi ApiKey), `STAFF` chỉ được tạo/sửa Campaign và Item,
  **không** được quản lý ApiKey hay mời thêm User khác
- `User` có `active = false` → đăng nhập bị từ chối dù đúng mật khẩu
- **Không liên quan đến `Participant`** (mục 4.6) — `User` là tài khoản
  nội bộ của Tenant, `Participant` là end-user mua hàng. Hai khái niệm
  không được dùng lẫn cho nhau.

### 4.4. Campaign
Đại diện cho 1 chiến dịch Flash Sale.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh |
| tenant | Tenant | Tenant sở hữu campaign (quan hệ N-1) |
| code | String | Mã định danh, duy nhất trong phạm vi 1 tenant (vd: `FS_12_12`) |
| name | String | Tên chiến dịch |
| status | Enum | `ACTIVE`, `CANCELLED` (xem ghi chú bên dưới) |
| startTime | Timestamp | Thời điểm bắt đầu |
| endTime | Timestamp | Thời điểm kết thúc |
| createdAt | Timestamp | Ngày tạo |

**Quy tắc nghiệp vụ:**
- `startTime` phải nhỏ hơn `endTime` (validate khi tạo)
- `code` duy nhất trong phạm vi 1 tenant (2 tenant khác nhau được phép
  trùng `code`)
- Field `status` trong DB **chỉ dùng để đánh dấu `CANCELLED`** (hủy thủ
  công bởi tenant). Trạng thái hiển thị thực tế cho client luôn được suy
  ra tại thời điểm xử lý request, theo thứ tự ưu tiên:
  1. Nếu `status = CANCELLED` trong DB → trả về `CANCELLED`
  2. Ngược lại, so sánh thời gian hiện tại với `startTime`/`endTime`:
     - now < startTime → `DRAFT` (chưa bắt đầu, không cho mua)
     - startTime ≤ now ≤ endTime → `ACTIVE` (đang diễn ra, cho phép mua)
     - now > endTime → `ENDED` (đã kết thúc, không cho mua)
- Chỉ campaign có trạng thái hiệu lực (suy ra) là `ACTIVE` mới chấp nhận
  request mua hàng
- Một Tenant chỉ được thao tác (xem/sửa) trên Campaign thuộc về chính
  mình

### 4.5. FlashSaleItem
Một sản phẩm cụ thể được bán trong 1 Campaign.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh |
| campaign | Campaign | Campaign chứa sản phẩm này (quan hệ N-1) |
| itemCode | String | Mã sản phẩm |
| itemName | String | Tên sản phẩm |
| originalPrice | Long | Giá gốc (VNĐ) |
| salePrice | Long | Giá flash sale (VNĐ) |
| totalQuantity | Integer | Tổng số lượng mở bán |
| remainingQuantity | Integer | Số lượng còn lại |
| active | Boolean | Cho phép tenant ẩn sản phẩm dù còn hàng |

**Quy tắc nghiệp vụ:**
- Một Campaign có thể chứa **nhiều** FlashSaleItem (nhiều SKU)
- `salePrice` phải nhỏ hơn `originalPrice`
- `remainingQuantity` khởi tạo bằng `totalQuantity`, chỉ giảm dần, không
  bao giờ âm
- `remainingQuantity = 0` hoặc `active = false` → từ chối mọi request mua
  sản phẩm này

### 4.6. Participant
Đại diện cho 1 end-user trong phạm vi 1 Tenant. BaaS **không lưu thông
tin cá nhân** (tên, SĐT, email...) của end-user — chỉ lưu một định danh
tham chiếu do Tenant cung cấp, nhằm phục vụ việc kiểm tra "user này đã
mua chưa" và truy vấn lịch sử.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh nội bộ trong BaaS |
| tenant | Tenant | Tenant sở hữu participant này (quan hệ N-1) |
| externalCustomerId | String | Định danh end-user do hệ thống Tenant cung cấp |
| createdAt | Timestamp | Lần đầu tiên participant này xuất hiện trong hệ thống |

**Quy tắc nghiệp vụ:**
- Cặp `(tenantId, externalCustomerId)` là duy nhất — 2 tenant khác nhau
  được phép có cùng `externalCustomerId` (vì đó là 2 hệ khách hàng độc
  lập)
- Khi Tenant gọi API mua hàng với 1 `externalCustomerId` chưa từng xuất
  hiện, hệ thống **tự động tạo mới** Participant tương ứng (không cần
  bước đăng ký riêng)

### 4.7. Order
Bản ghi kết quả của 1 lần thử mua hàng.

| Thuộc tính | Kiểu | Mô tả |
|---|---|---|
| id | Long | Định danh |
| tenant | Tenant | Tenant sở hữu (quan hệ N-1, lưu trực tiếp để cô lập dữ liệu nhanh, không cần join qua campaign) |
| campaign | Campaign | Campaign liên quan (quan hệ N-1) |
| item | FlashSaleItem | Sản phẩm được mua (quan hệ N-1) |
| participant | Participant | End-user thực hiện thao tác (quan hệ N-1) |
| status | Enum | `SUCCESS`, `FAILED` |
| failReason | String (nullable) | Lý do thất bại, vd: `OUT_OF_STOCK`, `ALREADY_PURCHASED`, `CAMPAIGN_NOT_ACTIVE` |
| createdAt | Timestamp | Thời điểm xử lý |

**Quy tắc nghiệp vụ:**
- Mỗi lần gọi API mua hàng **luôn tạo ra 1 bản ghi Order**, kể cả khi thất
  bại — phục vụ mục đích audit/log đầy đủ lịch sử thử mua
- Một `(participantId, campaignId)` chỉ được phép có **tối đa 1 Order với
  status = SUCCESS**. Mọi lần thử mua tiếp theo trong cùng campaign (dù
  SKU khác) đều bị từ chối với lý do `ALREADY_PURCHASED`
- Ràng buộc trên được đảm bảo bằng **unique index ở tầng database** (dạng
  partial unique index: `UNIQUE(campaign_id, participant_id) WHERE
  status = 'SUCCESS'`) — đây là cơ chế chính chống race condition khi
  nhiều request đồng thời, không phụ thuộc vào logic ứng dụng

## 5. Luồng nghiệp vụ chính (Core Flows)

### 5.1. Tenant đăng ký và lấy API Key
```
1. Gọi POST /api/v1/tenants với {name, adminUsername, adminPassword}
2. Hệ thống tạo Tenant (active = true)
3. Hệ thống tạo User đầu tiên với role = OWNER, gắn vào Tenant vừa tạo
4. Hệ thống tự động sinh 1 ApiKey đầu tiên, trả về NGUYÊN VĂN keyValue
   (đây là lần DUY NHẤT giá trị gốc được trả về — DB chỉ lưu bản hash)
```

### 5.2. Tenant Admin đăng nhập (control plane)
```
1. Gọi POST /api/v1/admin/auth/login
   Body: { username, password }
2. Hệ thống kiểm tra:
   a. username tồn tại trong phạm vi tenant tương ứng?
   b. passwordHash khớp?
   c. User.active = true?
3. Thành công → trả về JWT (chứa userId, tenantId, role)
4. Các API control plane tiếp theo (/api/v1/admin/**) yêu cầu header
   Authorization: Bearer <JWT>, KHÔNG dùng X-API-Key
```

> **Phân biệt 2 luồng xác thực:** mục 5.2 (JWT) dùng cho thao tác *cấu
> hình* qua dashboard. Mục 5.3–5.4 bên dưới (API Key) dùng cho thao tác
> *vận hành* tốc độ cao — về mặt nghiệp vụ, Tenant có thể dùng JWT để tạo
> Campaign/Item qua dashboard, hoặc dùng API Key để làm điều tương tự từ
> hệ thống backend riêng của họ. Endpoint mua hàng (5.4) **chỉ** dùng
> API Key vì đây là luồng end-user, không có khái niệm "đăng nhập".

### 5.3. Tenant tạo Campaign
```
1. Tenant gọi POST /api/v1/campaigns kèm X-API-Key
   Body: { name, startTime, endTime }
2. Hệ thống validate: startTime < endTime
3. Tạo Campaign với status = DRAFT (suy ra từ thời gian)
```

### 5.4. Tenant thêm sản phẩm vào Campaign
```
1. Tenant gọi POST /api/v1/campaigns/{campaignId}/items kèm X-API-Key
   Body: { sku, name, originalPrice, salePrice, totalStock }
2. Hệ thống validate: salePrice < originalPrice, totalStock > 0
3. Tạo FlashSaleItem với remainingStock = totalStock
```

### 5.5. End-user mua hàng (luồng quan trọng nhất)
```
1. Tenant (thay mặt end-user) gọi:
   POST /api/v1/campaigns/{campaignId}/items/{itemId}/purchase
   Header: X-API-Key
   Body: { userId }

2. Hệ thống kiểm tra theo thứ tự, dừng ngay khi gặp lỗi đầu tiên:
   a. API Key hợp lệ và thuộc đúng Tenant sở hữu Campaign?
      → Sai: 401/403
   b. Campaign có đang ACTIVE không? (now nằm trong [startTime, endTime])
      → Không: tạo Order(FAILED, CAMPAIGN_NOT_ACTIVE), trả lỗi 409
   c. Item có thuộc đúng Campaign không, và remainingStock > 0?
      → Hết hàng: tạo Order(FAILED, OUT_OF_STOCK), trả lỗi 409
   d. userId này đã có Order SUCCESS nào trong Campaign này chưa?
      → Đã có: tạo Order(FAILED, ALREADY_PURCHASED), trả lỗi 409
   e. Tất cả hợp lệ:
      → Giảm remainingStock của Item đi 1 (atomic, chống race condition)
      → Tạo Order(SUCCESS)
      → Trả về 200 kèm thông tin Order

3. Toàn bộ bước 2d–2e phải nằm trong 1 transaction với cơ chế khóa phù
   hợp (xem mục 6) để đảm bảo tính đúng đắn khi có nhiều request đồng
   thời cho cùng 1 Item.
```

**Ghi chú phạm vi:** giai đoạn đầu, `quantity` mặc định luôn là `1`
(do rule "mỗi user chỉ mua 1 lần/campaign" khiến việc mua nhiều số lượng
trong 1 lần không có nhiều ý nghĩa thực tế). Trường `quantity` vẫn được
giữ trong model để dễ mở rộng sau này nếu đổi rule.

### 5.6. Tra cứu
```
- GET /api/v1/campaigns/{campaignId}                  → chi tiết campaign
- GET /api/v1/campaigns/{campaignId}/items             → danh sách sản phẩm + remainingStock
- GET /api/v1/campaigns/{campaignId}/orders?userId=... → lịch sử mua của 1 user (kiểm tra đã mua chưa, mua gì)
- GET /api/v1/admin/me                                  → thông tin Tenant Admin đang đăng nhập (yêu cầu JWT)
```

## 6. Yêu cầu phi chức năng liên quan đến nghiệp vụ

| Yêu cầu | Mô tả |
|---|---|
| **Chống oversell** | Không được để `remainingStock` giảm xuống dưới 0 dù có hàng nghìn request đồng thời cho cùng 1 Item |
| **Chống mua trùng** | Không được để 1 `(userId, campaignId)` có 2 Order SUCCESS cùng lúc, kể cả khi 2 request gửi đồng thời (race condition) |
| **Cô lập theo Tenant** | Tenant A không được xem/sửa dữ liệu (Campaign, Item, Order) của Tenant B trong bất kỳ trường hợp nào |
| **Audit đầy đủ** | Mọi lần thử mua — kể cả thất bại — đều phải để lại dấu vết (Order record) |

> Việc lựa chọn cơ chế kỹ thuật cụ thể để đảm bảo các yêu cầu trên
> (DB-level locking, unique constraint, Redis, v.v.) thuộc về thiết kế kỹ
> thuật, nằm ngoài phạm vi tài liệu nghiệp vụ này.

## 7. Mã lỗi nghiệp vụ chuẩn hóa

| Mã lỗi | HTTP Status | Khi nào xảy ra |
|---|---|---|
| `INVALID_API_KEY` | 401 | API Key không tồn tại hoặc đã bị revoke |
| `TENANT_SUSPENDED` | 403 | Tenant bị tạm ngưng |
| `FORBIDDEN_RESOURCE` | 403 | Tenant cố truy cập tài nguyên không thuộc về mình |
| `CAMPAIGN_NOT_ACTIVE` | 409 | Campaign chưa bắt đầu hoặc đã kết thúc |
| `OUT_OF_STOCK` | 409 | Sản phẩm đã hết hàng |
| `ALREADY_PURCHASED` | 409 | User đã mua thành công 1 lần trong campaign này |
| `ITEM_NOT_FOUND` | 404 | Item không tồn tại hoặc không thuộc campaign chỉ định |
| `VALIDATION_ERROR` | 400 | Dữ liệu đầu vào không hợp lệ (vd: startTime > endTime) |
| `INVALID_CREDENTIALS` | 401 | Sai username/password khi Tenant Admin đăng nhập |
| `USER_INACTIVE` | 403 | Tài khoản Tenant Admin bị khóa (`active = false`) |
| `INVALID_TOKEN` | 401 | JWT không hợp lệ, hết hạn, hoặc thiếu khi gọi API control plane |
| `INSUFFICIENT_ROLE` | 403 | `STAFF` cố thực hiện hành động chỉ dành cho `OWNER` (vd: quản lý ApiKey) |

## 8. Ví dụ kịch bản demo end-to-end

```
1. Tạo Tenant "Shop ABC"                    → nhận API Key: sk_abc123
2. Tạo Campaign "Sale 11.11"
   startTime = 2026-06-21T10:00:00Z
   endTime   = 2026-06-21T10:05:00Z         (set ngắn để demo dễ test)
3. Thêm Item vào campaign:
   - SKU "TAI-NGHE-01", giá gốc 500k, giá sale 199k, stock = 5
   - SKU "SAC-DP-02",   giá gốc 300k, giá sale 99k,  stock = 3
4. Trước 10:00 → gọi purchase → lỗi CAMPAIGN_NOT_ACTIVE
5. Trong khung giờ, nhiều userId khác nhau gọi purchase đồng thời
   cho "TAI-NGHE-01" → chỉ đúng 5 request đầu thành công, còn lại
   OUT_OF_STOCK
6. Cùng 1 userId gọi purchase lần 2 (dù sang SKU "SAC-DP-02")
   → lỗi ALREADY_PURCHASED
7. Sau 10:05 → gọi purchase → lỗi CAMPAIGN_NOT_ACTIVE
```

Kịch bản này dùng để viết test tích hợp (integration test) và làm UI demo
trực quan.
