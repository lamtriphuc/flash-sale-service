package com.lamtriphuc.backend.common.exception;

import com.lamtriphuc.backend.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 1. Xử lý các Exception nghiệp vụ do Developer chủ động ném ra (AppException)
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // 2. Xử lý lỗi Validate dữ liệu (từ @Valid trong Controller)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handlingValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        // Trích xuất từng field bị lỗi và message tương ứng
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        ApiResponse<Map<String, String>> apiResponse = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                errors // Trả về object chứa các field lỗi cho Frontend báo đỏ
        );

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // 3. Xử lý lỗi Database (Ví dụ: vi phạm khóa Unique - Trùng Idempotency Key)
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handlingDataIntegrityViolationException(DataIntegrityViolationException exception) {
        log.error("Database Error: ", exception);
        ErrorCode errorCode = ErrorCode.DATABASE_ERROR;

        // Bắt lỗi trùng Idempotency Key hoặc Duplicate User
        if (exception.getMessage().contains("uk_orders_idempotency") || exception.getMessage().contains("uk_tenant_idempotency")) {
            errorCode = ErrorCode.IDEMPOTENCY_KEY_DUPLICATED;
        }

        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // 4. Xử lý lỗi 404 (Gọi sai endpoint API)
    @ExceptionHandler(value = NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlingNoHandlerFoundException(NoHandlerFoundException exception) {
        ErrorCode errorCode = ErrorCode.NOT_FOUND;
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getCode(), "Đường dẫn API không tồn tại");
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // 5. Xử lý TẤT CẢ các Exception còn lại (NullPointer, lỗi cấu hình, v.v.) - Bắt buộc phải có
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handlingRuntimeException(Exception exception) {
        log.error("Unhandled Exception: ", exception);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }
}