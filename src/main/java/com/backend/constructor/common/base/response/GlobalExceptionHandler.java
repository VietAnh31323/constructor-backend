package com.backend.constructor.common.base.response;

import com.backend.constructor.common.error.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response> handleBusinessException(BusinessException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getMessage(),
                null,
                null
        );
        response.addError(ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 2. Validation @Valid trong body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationException(MethodArgumentNotValidException ex) {
        ErrorResponse response = new ErrorResponse("Dữ liệu không hợp lệ", null, null);

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String code = "validation." + fieldError.getField();
            response.addError(code, fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Response> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse response = new ErrorResponse("Dữ liệu không hợp lệ", null, null);

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String code = "validation." + field;
            response.addError(code, violation.getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleUnexpectedException(Exception ex) {
        log.error("Lỗi hệ thống không xác định", ex);

        ErrorResponse response = new ErrorResponse(
                "Có lỗi xảy ra, xin vui lòng thử lại!",
                null,
                null
        );
        response.addError("error.systemError", "Hệ thống đang bận, vui lòng thử lại sau");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}