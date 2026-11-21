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
public class DashboardOverviewDto {
    private long totalProjects;
    private long activeProjects;
    private long overdueProjects;
    private long overdueTasks;
    private long pendingApprovalTasks;
    private long myOpenTasksCount;

    private BigDecimal totalContractValue;
    private BigDecimal totalCollectedAmount;
    private BigDecimal receivableTotal;
    private BigDecimal collectedThisMonth;

    private List<StateCountDto> projectsByState;
    private List<MonthlyCashflowDto> monthlyCashflow;
    private List<DashboardAlertDto> alerts;
    private List<MyTaskItemDto> myTasks;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StateCountDto {
        private String state;
        private String label;
        private long count;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyCashflowDto {
        private String month;
        private BigDecimal collectedAmount;
        private BigDecimal plannedAmount;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashboardAlertDto {
        private String alertType; // OVERDUE_PROJECT | OVERDUE_TASK | PAYMENT_DUE | PENDING_APPROVAL
        private String severity;  // ERROR | WARNING | INFO
        private String title;
        private String subtitle;
        private LocalDate dueDate;
        private String link;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyTaskItemDto {
        private Long id;
        private String code;
        private String name;
        private Long projectId;
        private String projectName;
        private String state;
        private String stateLabel;
        private BigDecimal progressPercent;
        private LocalDate endDate;
        private boolean overdue;
    }
}
