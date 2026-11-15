package com.backend.constructor.app.dto.customer;

import com.backend.constructor.core.domain.enums.CustomerPipelineStage;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPipelineSummaryDto {
    private long totalCustomers;
    private long individualCustomers;
    private long businessCustomers;
    private long upcomingFollowUps;
    private long overdueFollowUps;
    private BigDecimal totalContractValue;
    private List<StageItem> stages;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageItem {
        private CustomerPipelineStage stage;
        private String label;
        private int stepOrder;
        private long customerCount;
        private long projectCount;
        private BigDecimal totalContractValue;
    }
}
