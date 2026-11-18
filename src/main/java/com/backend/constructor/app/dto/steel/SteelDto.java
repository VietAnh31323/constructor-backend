package com.backend.constructor.app.dto.steel;

import com.backend.constructor.app.dto.upload.UploadDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelDto {
    private Long id;

    private Long assemblyMapId;

    private Long steelCategoryId;

    private List<UploadDto> images;

    private Long barCode;

    private String barLabel;

    @NotNull
    private Long barQuantity;

    private BigDecimal spliceLength;

    private BigDecimal bendDeduction;

    @NotNull
    private BigDecimal barDiameter;

    private Integer sortOrder;

    private String note;

    @Valid
    @NotEmpty
    private List<SteelLineDto> steelLines;
}
