package com.backend.constructor.app.dto.project;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPaymentSummaryDto {
    private Long projectId;
    private BigDecimal contractValue;
    private BigDecimal contractAdvance;
    private BigDecimal confirmedPaymentTotal;
    private BigDecimal pendingPaymentTotal;
    private BigDecimal totalCollected;
    private BigDecimal remainingAmount;
    private BigDecimal collectionRate;
    private List<ProjectPaymentDto> payments;
}
