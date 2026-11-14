package com.backend.constructor.common.error;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {
    private final String errorCode;

    public TooManyRequestsException(String message) {
        super(message);
        this.errorCode = "429";
    }

    public TooManyRequestsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
