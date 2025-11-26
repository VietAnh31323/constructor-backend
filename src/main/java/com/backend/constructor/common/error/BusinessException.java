package com.backend.constructor.common.error;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final String message;

    public static class Builder {
        private String errorCode = "";
        private String message;
        private Throwable cause;

        private Builder() {
        }

        public Builder code(String errorCode) {
            this.errorCode = errorCode != null ? errorCode : "";
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public BusinessException build() {
            BusinessException ex = new BusinessException(
                    this.errorCode.isEmpty() ? null : this.errorCode,
                    this.message
            );
            if (this.cause != null) {
                ex.initCause(this.cause);
            }
            return ex;
        }

        // Shortcut: throw ngay lập tức
        public BusinessException throwIt() {
            throw build();
        }
    }

    /**
     * Construct a new instance of {@code RestClientException} with the given message and
     * exception.
     *
     * @param msg the message
     */
    public BusinessException(String msg) {
        super(msg);
        this.errorCode = Strings.EMPTY;
        this.message = msg;
    }

    /**
     * Construct a new instance of {@code RestClientException} with the given message and
     * exception.
     *
     * @param errorCode the message
     * @param msg       the message
     * @param ex        the exception
     */
    public BusinessException(String errorCode, String msg, Throwable ex) {
        super(msg, ex);
        this.errorCode = errorCode;
        this.message = msg;
    }

    /**
     * Construct a new instance of {@code RestClientException} with the given message and
     * exception.
     *
     * @param errorCode the message
     * @param msg       the message
     */
    public BusinessException(String errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
        this.message = msg;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BusinessException exception(String message) {
        return builder().message(message).build();
    }

    public static BusinessException exception(String errorCode, String message) {
        return builder().code(errorCode).message(message).build();
    }

    public static BusinessException exception(String errorCode, String message, Throwable cause) {
        return builder().code(errorCode).message(message).cause(cause).build();
    }

    public static void throwException(String message) {
        throw exception(message);
    }

    public static void throwException(String errorCode, String message) {
        throw exception(errorCode, message);
    }

    public static void throwException(String errorCode, String message, Throwable cause) {
        throw exception(errorCode, message, cause);
    }
}
