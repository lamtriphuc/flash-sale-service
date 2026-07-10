package com.lamtriphuc.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // Nhóm 200: Thành công
    SUCCESS(200, HttpStatus.OK, "Thành công"),

    // Nhóm 400: Lỗi từ phía Client (Frontend truyền sai)
    BAD_REQUEST(400, HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    VALIDATION_ERROR(400, HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
    IDEMPOTENCY_KEY_DUPLICATED(400, HttpStatus.BAD_REQUEST, "Giao dịch đang được xử lý, vui lòng không thử lại"),

    // Nhóm 401 & 403: Lỗi xác thực và phân quyền
    UNAUTHENTICATED(401, HttpStatus.UNAUTHORIZED, "Chưa xác thực danh tính"),
    UNAUTHORIZED(403, HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này"),

    // Nhóm 404: Không tìm thấy
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    TENANT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin Tenant"),
    CAMPAIGN_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Chiến dịch Flash Sale không tồn tại"),
    PRODUCT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"),

    // Nhóm Logic Nghiệp Vụ Flash Sale (409 Conflict hoặc 400)
    OUT_OF_STOCK(409, HttpStatus.CONFLICT, "Sản phẩm đã hết hàng"),
    CAMPAIGN_NOT_STARTED(409, HttpStatus.BAD_REQUEST, "Chiến dịch chưa bắt đầu"),
    CAMPAIGN_ENDED(409, HttpStatus.BAD_REQUEST, "Chiến dịch đã kết thúc"),
    ALREADY_BOUGHT(409, HttpStatus.CONFLICT, "Bạn đã tham gia mua sản phẩm này rồi"),

    // Nhóm 500: Lỗi hệ thống Backend
    INTERNAL_SERVER_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống nội bộ"),
    DATABASE_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi truy xuất cơ sở dữ liệu");

    private final int code;
    private final HttpStatusCode statusCode;
    private final String message;

    ErrorCode(int code, HttpStatusCode statusCode, String message) {
        this.code = code;
        this.statusCode = statusCode;
        this.message = message;
    }
}