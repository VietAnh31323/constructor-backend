package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum AddendumAdjustmentType {
    INCREASE("Phát sinh tăng giá trị"),
    DECREASE("Giảm trừ khối lượng / giá trị"),
    EXTEND_TIME("Gia hạn tiến độ"),
    SCOPE_CHANGE("Điều chỉnh phạm vi & điều khoản"),
    OTHER("Khác");

    private final String label;

    AddendumAdjustmentType(String label) {
        this.label = label;
    }
}
