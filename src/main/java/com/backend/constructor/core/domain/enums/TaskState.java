package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 2.23: Workflow trạng thái công việc
 * Chưa làm → Đang làm → Chờ duyệt → Hoàn thành / Trả lại
 */
@Getter
@AllArgsConstructor
public enum TaskState {
    NOT_STARTED("Chưa làm", 0),
    IN_PROGRESS("Đang làm", 50),
    PENDING_REVIEW("Chờ duyệt", 90),
    RETURNED("Trả lại", 50),
    COMPLETED("Hoàn thành", 100);

    private final String label;
    /** % tiến độ tự động cho công việc lá (2.22 - không nhập tay) */
    private final int defaultPercent;

    /** Công việc đang được thực hiện (chưa kết thúc) */
    public boolean isActive() {
        return this == IN_PROGRESS || this == PENDING_REVIEW || this == RETURNED;
    }
}
