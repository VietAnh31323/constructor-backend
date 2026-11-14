package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum Position {
    ARCHITECT("Kiến trúc sư", false),
    STRUCTURAL_ENGINEER("Kỹ sư kết cấu", false),
    MEP_ENGINEER("Kỹ sư cơ điện (MEP)", false),
    SUPERVISOR("Kỹ sư giám sát", true),
    SITE_MANAGER("Chỉ huy trưởng công trình", true),
    PROJECT_MANAGER("Quản lý dự án (PM)", false),
    ESTIMATOR("Kỹ sư dự toán", false),
    ACCOUNTANT("Kế toán", false),
    WAREHOUSE_KEEPER("Thủ kho / Vật tư", false),
    ADMIN_STAFF("Hành chính - Nhân sự / CSKH", false);

    private final String label;
    private final boolean requiresCertificate;

    Position(String label, boolean requiresCertificate) {
        this.label = label;
        this.requiresCertificate = requiresCertificate;
    }
}
