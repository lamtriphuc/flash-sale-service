import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Cấu hình mức độ tấn công (Load Profile) ---
export const options = {
    // Kịch bản: 10 VUs, mỗi VU gửi 1 request/giây (tránh rate limit 5 req/s)
    scenarios: {
        flash_sale_spike: {
            executor: 'constant-vus',
            vus: 10,             // 10 Virtual Users
            duration: '10s',     // Giữ trong 10 giây
        },
    },
    // Định nghĩa các ngưỡng kỳ vọng
    thresholds: {
        'http_req_duration': ['p(95)<1000'], // 95% request phải phản hồi dưới 1s
        'http_req_failed': ['rate<0.05'],    // Tỉ lệ lỗi (5xx) phải dưới 5%
    },
};

//  Cấu hình Môi trường
const BASE_URL = 'http://192.168.1.56:8080/api/v1/orders/checkout';
const PRODUCT_ID = '34928cdc-59d0-4f1d-9f08-a8d13a29991a';
// API key hợp lệ
const API_KEY = 'pk_live_BRZwu92wko20eP6jGmD4ChUP4F6XvDXJEO8cND-q0Pk';

// Hành động của từng User Ảo (VU) ---
export default function () {
    // Chuẩn bị Payload
    const payload = JSON.stringify({
        productId: PRODUCT_ID,
        quantity: 1,
        // Dùng thư viện của k6 sinh IdempotencyKey duy nhất chống trùng đơn
        idempotencyKey: uuidv4()
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-API-Key': API_KEY, // Hoặc dùng 'Authorization': 'Bearer ...'
        },
    };

    // Bắn Request POST
    const res = http.post(BASE_URL, payload, params);

    // Kiểm tra kết quả trả về ---
    check(res, {
        // Thành công hoặc bị chặn đều là kết quả hợp lệ (không phải lỗi server)
        'is valid response (200 or 409)': (r) => r.status === 200 || r.status === 409,

        // Bị gác cổng Rate Limit tát ra? (Mã 429 Too Many Requests)
        'is rate limited (429)': (r) => r.status === 429,

        // Server có bị cháy không? (Mã 500)
        'is server error (500)': (r) => r.status >= 500,
    });

    sleep(Math.random() * 0.4 + 0.1);
}