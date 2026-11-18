package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SiteReportPhase {
    BEFORE("Trước thi công"),
    DURING("Trong thi công"),
    AFTER("Sau thi công");

    private final String label;
}
