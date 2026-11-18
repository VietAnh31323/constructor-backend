package com.backend.constructor.app.dto.steel;

import com.backend.constructor.app.dto.upload.UploadDto;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelOutput {
    private Long id;

    private Long assemblyMapId;

    private Long steelCategoryId;

    private String steelCategoryCode;

    private String steelCategoryName;

    private Long barCode;

    private String barLabel;

    private String assemblyName;

    private List<UploadDto> images;

    private BigDecimal barDiameter;

    private Long barQuantity;

    private BigDecimal sliceLength;

    private BigDecimal spliceLength;

    private BigDecimal bendDeduction;

    private BigDecimal segmentLength;

    private BigDecimal cutLength;

    private BigDecimal cutLengthMeter;

    private BigDecimal length;

    private BigDecimal unitWeight;

    private Long totalBars;

    private BigDecimal totalLength;

    private BigDecimal totalWeight;

    private Integer sortOrder;

    private String note;

    private List<SteelLineDto> steelLines;
}
