package com.backend.constructor.common.enums;

import lombok.Getter;

@Getter
public enum ERole {
    ADMIN("Quản trị viên"),
    DIRECTOR("Giám đốc"),
    PROJECT_MANAGER("Quản lý dự án (PM)"),
    SITE_ENGINEER("Kỹ sư giám sát / Chỉ huy trưởng"),
    ACCOUNTANT("Kế toán"),
    ESTIMATOR("Dự toán"),
    WAREHOUSE("Kho"),
    CUSTOMER_CARE("Chăm sóc khách hàng"),
    CUSTOMER("Khách hàng"),
    STAFF("Nhân viên");

    private final String value;

    ERole(String value) {
        this.value = value;
    }
}
