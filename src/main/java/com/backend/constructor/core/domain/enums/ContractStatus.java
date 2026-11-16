package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum ContractStatus {
    DRAFT("Dự thảo"),
    PENDING_APPROVAL("Chờ duyệt"),
    SIGNED("Đã ký kết"),
    LIQUIDATED("Đã nghiệm thu thanh lý"),
    TERMINATED("Đã chấm dứt");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }
}
