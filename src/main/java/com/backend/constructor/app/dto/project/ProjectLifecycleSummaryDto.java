package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectLifecycleSummaryDto {
    private long totalProjects;
    private long activeProjects;
    private long pausedProjects;
    private long cancelledProjects;
    private long closedProjects;
    private BigDecimal totalContractValue;
    private List<StageItem> stages;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StageItem {
        private ProjectState state;
        private String label;
        private int stepOrder;
        private long projectCount;
        private BigDecimal totalContractValue;
    }
}
