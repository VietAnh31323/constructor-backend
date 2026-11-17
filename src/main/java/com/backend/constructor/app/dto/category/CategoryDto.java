package com.backend.constructor.app.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryDto {
    private Long id;

    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private String description;

    private Boolean isActive;

    // 2.18: Hạng mục dạng cây
    private Long parentId;

    private String parentName;

    private String unit;

    private BigDecimal defaultUnitPrice;

    private Integer sortOrder;

    @Builder.Default
    private List<CategoryDto> children = new ArrayList<>();

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trim(this.description);
        this.unit = StringUtils.trimToNull(this.unit);
    }
}
