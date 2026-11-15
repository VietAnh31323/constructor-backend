package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum CustomerType {
    INDIVIDUAL("Cá nhân"),
    BUSINESS("Doanh nghiệp");

    private final String label;

    CustomerType(String label) {
        this.label = label;
    }
}
