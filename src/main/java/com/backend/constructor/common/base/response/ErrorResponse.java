// src/main/java/com/backend/constructor/common/base/response/ErrorResponse.java
package com.backend.constructor.common.base.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends Response {

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final List<ErrorDetail> errorCodes = new ArrayList<>();

    @Builder
    public ErrorResponse(String message, String traceId, List<ErrorDetail> errorCodes) {
        super("Có lỗi xảy ra, xin vui lòng thử lại!", traceId);
        if (errorCodes != null) {
            this.errorCodes.addAll(errorCodes);
        }
    }

    public void addError(String code, String message) {
        this.errorCodes.add(new ErrorDetail(code, message));
    }

    // Factory
    public static ErrorResponse fail(String code, String message) {
        ErrorResponse resp = new ErrorResponse("Có lỗi xảy ra, xin vui lòng thử lại!", null, null);
        resp.addError(code, message);
        return resp;
    }

    public static ErrorResponse fail(String userMessage, String code, String devMessage) {
        ErrorResponse resp = new ErrorResponse(userMessage, null, null);
        resp.addError(code, devMessage);
        return resp;
    }

    public record ErrorDetail(String code, String message) {
    }
}