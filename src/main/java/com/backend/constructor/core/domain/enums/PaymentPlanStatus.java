package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentPlanStatus {
    PLANNED("Chưa đến hạn"),
    DUE("Đến hạn thu"),
    PARTIAL("Thu một phần"),
    PAID("Đã thu đủ"),
    OVERDUE("Quá hạn");

    private final String label;
}
