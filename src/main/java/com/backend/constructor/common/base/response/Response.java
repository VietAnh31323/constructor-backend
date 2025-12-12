package com.backend.constructor.common.base.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Response {
    String message;
    String traceId;

    protected Response() {
        this.traceId = generateTraceId();
    }

    protected Response(String message) {
        this();
        this.message = message;
    }

    protected Response(String message, String traceId) {
        this.message = message;
        this.traceId = traceId != null ? traceId : generateTraceId();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}