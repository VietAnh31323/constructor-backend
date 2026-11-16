package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum ProjectMemberRole {
    PROJECT_MANAGER("Chỉ huy trưởng / Quản lý dự án (PM)", true),
    SUPERVISOR("Kỹ sư giám sát", false),
    SITE_ENGINEER("Kỹ sư hiện trường", false),
    STRUCTURAL_ENGINEER("Kỹ sư kết cấu", false),
    MEP_ENGINEER("Kỹ sư cơ điện (MEP)", false),
    ARCHITECT("Kiến trúc sư", false),
    ESTIMATOR("Kỹ sư dự toán", false),
    ACCOUNTANT("Kế toán dự án", false),
    SAFETY_OFFICER("Cán bộ an toàn lao động", false),
    CREATOR("Người tạo dự án", false),
    SUPPORTER("Nhân sự hỗ trợ", false);

    private final String label;
    private final boolean defaultLeader;

    ProjectMemberRole(String label, boolean defaultLeader) {
        this.label = label;
        this.defaultLeader = defaultLeader;
    }
}
