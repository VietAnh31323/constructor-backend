package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum CareOutcome {
    POSITIVE("Tích cực / Quan tâm"),
    NEED_FOLLOW_UP("Cần chăm sóc thêm"),
    NO_ANSWER("Không nghe máy / Chưa phản hồi"),
    DECLINED("Từ chối / Tạm hoãn");

    private final String label;

    CareOutcome(String label) {
        this.label = label;
    }
}
