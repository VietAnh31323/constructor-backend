package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    TASK_OVERDUE("Công việc trễ hạn"),
    TASK_SUBMITTED("Công việc chờ duyệt"),
    TASK_APPROVED("Công việc được duyệt"),
    TASK_RETURNED("Công việc bị trả lại"),
    TASK_MENTION("Được nhắc đến trong bình luận"),
    TASK_COMMENT("Bình luận mới"),
    TASK_SITE_REPORT("Báo cáo hiện trường mới");

    private final String label;
}
