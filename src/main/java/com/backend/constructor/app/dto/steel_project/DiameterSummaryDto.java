package com.backend.constructor.app.dto.steel_project;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiameterSummaryDto {
    private BigDecimal barDiameter;
    private Long totalBars;
    private BigDecimal totalLengthMeter;
    private BigDecimal totalWeightKg;
}
