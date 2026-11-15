package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum CustomerPipelineStage {
    LEAD("Tiềm năng", 1),
    QUOTED("Báo giá", 2),
    CONTRACT_SIGNED("Ký HĐ", 3),
    IN_CONSTRUCTION("Đang thi công", 4),
    WARRANTY("Bảo hành", 5),
    LOST("Ngưng theo dõi", 6);

    private final String label;
    private final int stepOrder;

    CustomerPipelineStage(String label, int stepOrder) {
        this.label = label;
        this.stepOrder = stepOrder;
    }
}
