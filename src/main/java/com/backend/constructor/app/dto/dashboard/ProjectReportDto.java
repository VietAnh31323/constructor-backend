package com.backend.constructor.app.dto.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectReportDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String customerName;
    private String state;
    private String stateLabel;
    private LocalDate signDate;
    private LocalDate deliveryDate;
    private boolean overdue;

    private BigDecimal progressPercent;
    private BigDecimal contractValue;
    private BigDecimal collectedAmount;
    private BigDecimal remainingReceivable;
    private BigDecimal estimatedCost;
    private BigDecimal estimatedProfit;
    private BigDecimal profitMarginPercent;

    private BigDecimal totalSteelWeightKg;
    private Integer completedPhases;
    private Integer totalPhases;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SummaryResponse {
        private BigDecimal totalContractValue;
        private BigDecimal totalCollected;
        private BigDecimal totalReceivable;
        private BigDecimal totalEstimatedCost;
        private BigDecimal totalEstimatedProfit;
        private BigDecimal averageProgressPercent;
        private List<ProjectReportDto> projects;
    }
}
