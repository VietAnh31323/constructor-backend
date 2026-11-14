package com.backend.constructor.app.dto.staff;

import com.backend.constructor.core.domain.enums.PriorityLevel;
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
public class StaffTaskItemDto {
    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String parentTaskCode;
    private String parentTaskName;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long progressId;
    private String progressName;
    private List<String> rolesInTask;
    private PriorityLevel priorityLevel;
    private TaskState state;
    private BigDecimal progressPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean overdue;
    private boolean dueSoon;
}
