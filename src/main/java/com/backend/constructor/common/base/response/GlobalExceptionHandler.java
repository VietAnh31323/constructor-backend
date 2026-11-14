package com.backend.constructor.common.base.response;

import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.error.TooManyRequestsException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final HandleMessage handleMessage;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Cảnh báo bảo mật: Truy cập bị từ chối - {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                "Truy cập bị từ chối",
                null,
                null
        );
        response.addError("403", "Bạn không có quyền thực hiện tính năng này!");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Response> handleTooManyRequestsException(TooManyRequestsException ex) {
        log.warn("Giới hạn tần suất đăng nhập: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage(), null, null);
        response.addError(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Xác thực thất bại: {}", ex.getMessage());
        String msg = "Tài khoản hoặc mật khẩu không chính xác!";
        ErrorResponse response = new ErrorResponse(msg, null, null);
        response.addError("401", msg);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response> handleResponseStatusException(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ErrorResponse response = new ErrorResponse(reason, null, null);
        response.addError(String.valueOf(ex.getStatusCode().value()), reason);
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response> handleBusinessException(BusinessException ex) {
        String resolvedMsg = handleMessage.getMessage(ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                resolvedMsg,
                null,
                null
        );
        response.addError(ex.getErrorCode(), resolvedMsg);

        return ResponseEntity.ok().body(response);
    }

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Vi phạm ràng buộc cơ sở dữ liệu: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse("Dữ liệu đã tồn tại", null, null);
        response.addError("ERROR_0002", "Dữ liệu đã tồn tại trong hệ thống (trùng lặp mã hoặc tên đăng nhập)!");
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
