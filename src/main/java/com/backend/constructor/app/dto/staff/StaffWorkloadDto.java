package com.backend.constructor.app.dto.staff;

import com.backend.constructor.core.domain.enums.Department;
import com.backend.constructor.core.domain.enums.Position;
import com.backend.constructor.core.domain.enums.WorkloadLevel;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffWorkloadDto {
    private Long staffId;
    private String staffCode;
    private String staffName;
    private Position position;
    private Department department;

    private int totalProjects;
    private int activeProjects;
    private int managedActiveProjects;
    private int completedProjects;

    private int totalTasks;
    private int notStartedTasks;
    private int inProgressTasks;
    private int completedTasks;
    private int activeTasks;
    private int overdueTasks;
    private int dueSoonTasks;
    private int highPriorityActiveTasks;

    private WorkloadLevel workloadLevel;

    private List<StaffProjectItemDto> projects;
    private List<StaffTaskItemDto> tasks;
}
