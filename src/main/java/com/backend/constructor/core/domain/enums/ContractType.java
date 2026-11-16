package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum ContractType {
    MAIN("Hợp đồng chính"),
    PRINCIPLE("Hợp đồng nguyên tắc"),
    SUB_CONTRACT("Hợp đồng thầu phụ"),
    CONSULTING("Hợp đồng tư vấn / thiết kế");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }
}
