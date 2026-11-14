package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum Department {
    BOARD_OF_DIRECTORS("Ban giám đốc"),
    ARCHITECTURE("Phòng Kiến trúc - Thiết kế"),
    ENGINEERING("Phòng Kỹ thuật - Thi công"),
    SUPERVISION("Ban Giám sát công trình"),
    ESTIMATION("Phòng Dự toán - Đấu thầu"),
    FINANCE_ACCOUNTING("Phòng Tài chính - Kế toán"),
    PROCUREMENT_WAREHOUSE("Phòng Vật tư - Kho"),
    HR_ADMIN("Phòng Hành chính - Nhân sự"),
    CUSTOMER_SERVICE("Phòng Kinh doanh - CSKH");

    private final String label;

    Department(String label) {
        this.label = label;
    }
}
