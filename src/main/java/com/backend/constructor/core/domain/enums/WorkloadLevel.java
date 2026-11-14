package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum WorkloadLevel {
    NORMAL("Bình thường"),
    HIGH("Tải cao"),
    OVERLOADED("Quá tải");

    private final String label;

    WorkloadLevel(String label) {
        this.label = label;
    }
}
