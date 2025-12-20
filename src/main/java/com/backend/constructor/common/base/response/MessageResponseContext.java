package com.backend.constructor.common.base.response;

import java.util.Objects;

public final class MessageResponseContext {

    private static final ThreadLocal<String> MESSAGE_HOLDER = new InheritableThreadLocal<>();

    private MessageResponseContext() {
    }

    public static void setResourceMessage(String message) {
        MESSAGE_HOLDER.set(message);
    }

    public static String getResourceMessage() {
        return MESSAGE_HOLDER.get();
    }

    public static void clear() {
        if (Objects.nonNull(MESSAGE_HOLDER.get())) {
            MESSAGE_HOLDER.remove();
        }
    }
}
