// src/main/java/com/backend/constructor/common/base/response/SuccessResponse.java
package com.backend.constructor.common.base.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> extends Response {

    private final T data;

    @Builder
    public SuccessResponse(String message, String traceId, T data) {
        super(message != null ? message : "Thành công!", traceId);
        this.data = data;
    }

    // Factory methods – dùng cực tiện
    public static <T> SuccessResponse<T> ok(T data) {
        return SuccessResponse.<T>builder()
                .data(data)
                .build();
    }

    public static <T> SuccessResponse<T> ok(String message, T data) {
        return SuccessResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }
}