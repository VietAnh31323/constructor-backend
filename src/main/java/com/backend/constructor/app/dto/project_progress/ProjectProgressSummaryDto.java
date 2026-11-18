package com.backend.constructor.app.dto.project_progress;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 2.24: Tổng hợp tiến độ theo dự án cho màn Quản lý tiến độ.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectProgressSummaryDto {
    private Long projectId;
    private BigDecimal progressPercent;
    private int phaseCount;
    private int totalTasks;
    private int notStartedTasks;
    private int inProgressTasks;
    private int pendingReviewTasks;
    private int returnedTasks;
    private int completedTasks;
    private int overdueTasks;
    private LocalDate plannedEndDate;
    private LocalDate nextDeadline;
    private Long delayDays;
    private String scheduleStatus;
}
