package com.backend.constructor.core.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildingType {
    RESIDENTIAL("Nhà ở dân dụng"),
    VILLA("Biệt thự / Nhà vườn"),
    TOWNHOUSE("Nhà phố / Liền kề"),
    COMMERCIAL("Công trình thương mại / Văn phòng"),
    INDUSTRIAL("Nhà xưởng / Công nghiệp"),
    INFRASTRUCTURE("Hạ tầng kỹ thuật"),
    INTERIOR("Nội thất / Cải tạo"),
    OTHER("Loại hình khác");

    private final String label;
}
