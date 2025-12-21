package com.backend.constructor.common.enums;

import lombok.Getter;

@Getter
public enum ERole {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên"),
    CUSTOMER_CARE("Chăm sóc khách hàng");
    private final String value;

    ERole(String value) {
        this.value = value;
    }
}
