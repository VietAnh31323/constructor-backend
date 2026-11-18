package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.ProjectState;
import com.backend.constructor.core.domain.enums.TaskState;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectGanttDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String customerName;
    private String managerName;
    private ProjectState state;
    private String stateLabel;
    private BigDecimal progressPercent;
    private LocalDate signDate;
    private LocalDate deliveryDate;
    private LocalDate actualHandoverDate;
    private LocalDate warrantyEndDate;
    private LocalDate timelineStartDate;
    private LocalDate timelineEndDate;
    private int totalPhases;
    private int totalTasks;
    private int completedTasks;
    private int inProgressTasks;
    private int notStartedTasks;
    private int overdueTasks;
    // 2.23 / 2.26
    private int pendingReviewTasks;
    private int returnedTasks;
    private int atRiskTasks;
    private String scheduleStatus;
    private long delayDays;
    private List<PhaseItem> phases;
    private List<MilestoneItem> milestones;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PhaseItem {
        private Long projectProgressId;
        private Long progressId;
        private String progressCode;
        private String progressName;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal progressPercent;
        private int taskCount;
        private int completedTaskCount;
        // 2.21: thứ tự, kế hoạch / thực tế, trọng số
        private Integer sortOrder;
        private LocalDate plannedStartDate;
        private LocalDate plannedEndDate;
        private LocalDate actualStartDate;
        private LocalDate actualEndDate;
        private BigDecimal weight;
        private String note;
        // 2.26: so sánh kế hoạch – thực tế
        private int overdueTaskCount;
        private long delayDays;
        private String scheduleStatus;
        private List<TaskItem> tasks;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskItem {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Long parentId;
        private Long projectProgressId;
        private LocalDate startDate;
        private LocalDate endDate;
        private long durationDays;
        private Long remainDays;
        private BigDecimal progressPercent;
        private TaskState state;
        private String stateLabel;
        private PriorityLevel priorityLevel;
        private boolean overdue;
        private CodeNameResponse reviewer;
        private List<CodeNameResponse> assignees;
        // 2.22: trọng số
        private BigDecimal weight;
        // 2.26: kế hoạch – thực tế
        private LocalDate actualStartDate;
        private LocalDate actualEndDate;
        private long delayDays;
        private String scheduleStatus;
        // 2.23: workflow
        private String returnReason;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private boolean hasChildren;
        private boolean canReview;
        private List<TaskState> allowedNextStates;
        // 2.25 / 2.27
        private long siteReportCount;
        private long commentCount;
        private List<TaskItem> subTasks;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MilestoneItem {
        private String key;
        private String type; // CONTRACT | PAYMENT | LIFECYCLE
        private String title;
        private String subtitle;
        private LocalDate date;
        private BigDecimal amount;
        private String status; // DONE | UPCOMING | OVERDUE | INFO
    }
}
