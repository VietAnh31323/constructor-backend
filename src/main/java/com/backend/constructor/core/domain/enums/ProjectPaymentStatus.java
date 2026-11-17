package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectPaymentStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận thu"),
    CANCELLED("Đã hủy");

    private final String label;
}
