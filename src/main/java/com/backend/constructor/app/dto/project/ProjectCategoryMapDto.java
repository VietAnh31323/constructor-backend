package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCategoryMapDto {
    private Long id;

    @Valid
    @NotNull
    private CodeNameResponse category;

    // 2.19: Khối lượng & đơn giá hạng mục
    private String unit;

    @Min(0)
    private BigDecimal quantity;

    @Min(0)
    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private String note;
}
