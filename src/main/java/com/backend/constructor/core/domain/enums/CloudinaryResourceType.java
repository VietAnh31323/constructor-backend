package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum CloudinaryResourceType {
    IMAGE("image"),
    VIDEO("video"),
    RAW("raw");

    private final String value;

    CloudinaryResourceType(String value) {
        this.value = value;
    }

}
