package com.backend.constructor.core.domain.enums;

public enum CloudinaryResourceType {
    IMAGE("image"),
    VIDEO("video"),
    RAW("raw");

    private final String value;

    CloudinaryResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
