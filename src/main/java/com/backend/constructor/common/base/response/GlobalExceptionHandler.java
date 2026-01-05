package com.backend.constructor.common.base.response;

import com.backend.constructor.common.error.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final HandleMessage handleMessage;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDeniedException(AccessDeniedException ex) {
        // Log lỗi nếu cần thiết
        log.warn("Cảnh báo bảo mật: Truy cập bị từ chối - {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                "Truy cập bị từ chối", // Tiêu đề chính của lỗi
                null,
                null
        );

        // Bắn ra message "đẹp" theo ý bạn
        String customMessage = "Bạn không có quyền thực hiện tính năng này!";

        // Giả sử mã lỗi 403 là mã mặc định cho lỗi phân quyền
        response.addError("403", customMessage);

        return ResponseEntity.status(403).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response> handleBusinessException(BusinessException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                null,
                null
        );
        response.addError(ex.getErrorCode(), handleMessage.getMessage(ex.getMessage(), ex));

        return ResponseEntity.ok().body(response);
    }

    // 2. Validation @Valid trong body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationException(MethodArgumentNotValidException ex) {
        ErrorResponse response = new ErrorResponse("Dữ liệu không hợp lệ", null, null);

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String code = fieldError.getField();
            response.addError(code, fieldError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse response = new ErrorResponse("Dữ liệu không hợp lệ", null, null);

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String code = violation.getPropertyPath().toString();
            response.addError(code, violation.getMessage());
        }

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleUnexpectedException(Exception ex) {
        log.error("Lỗi hệ thống không xác định", ex);

        ErrorResponse response = new ErrorResponse(
                "Có lỗi xảy ra, xin vui lòng thử lại!",
                null,
                null
        );
        response.addError("500", ex.getMessage());

        return ResponseEntity.internalServerError().body(response);
    }
}