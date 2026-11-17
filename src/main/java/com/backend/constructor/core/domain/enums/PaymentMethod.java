package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    BANK_TRANSFER("Chuyển khoản"),
    CASH("Tiền mặt"),
    OTHER("Hình thức khác");

    private final String label;
}
