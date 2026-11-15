package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum CareChannel {
    CALL("Cuộc gọi điện thoại"),
    MEETING("Gặp trực tiếp"),
    SITE_SURVEY("Khảo sát thực tế"),
    ZALO_SMS("Zalo / Tin nhắn"),
    EMAIL("Email"),
    OTHER("Khác");

    private final String label;

    CareChannel(String label) {
        this.label = label;
    }
}
