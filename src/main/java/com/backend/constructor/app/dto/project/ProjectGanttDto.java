package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.ProjectState;
import com.backend.constructor.core.domain.enums.TaskState;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        private PriorityLevel priorityLevel;
        private boolean overdue;
        private CodeNameResponse reviewer;
        private List<CodeNameResponse> assignees;
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
