package com.backend.constructor.core.domain.enums;

import lombok.Getter;

@Getter
public enum CertificateType {
    CONSTRUCTION_SUPERVISION("Giám sát thi công xây dựng"),
    SITE_MANAGEMENT("Chỉ huy trưởng công trường"),
    ARCHITECTURAL_DESIGN("Thiết kế kiến trúc công trình"),
    STRUCTURAL_DESIGN("Thiết kế kết cấu công trình"),
    MEP_DESIGN("Thiết kế cơ - điện công trình (MEP)"),
    CONSTRUCTION_VALUATION("Định giá xây dựng / Dự toán"),
    SURVEYING("Khảo sát xây dựng"),
    PROJECT_MANAGEMENT("Quản lý dự án đầu tư xây dựng"),
    SAFETY_LABOR("An toàn vệ sinh lao động"),
    OTHER("Chứng chỉ khác");

    private final String label;

    CertificateType(String label) {
        this.label = label;
    }
}
