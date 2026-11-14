package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum StaffStatus {
    WORKING("Đang làm việc"),
    PROBATION("Thử việc"),
    ON_LEAVE("Tạm nghỉ"),
    RESIGNED("Đã nghỉ việc");

    private final String label;

    StaffStatus(String label) {
        this.label = label;
    }
}
