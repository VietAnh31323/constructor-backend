package com.backend.constructor.app.dto.task;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
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
public class TaskOutput {
    private Long id;

    private String code;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long remainDay;

    private ReviewerDto reviewer;

    private List<CodeNameResponse> staffs;

    private BigDecimal progressPercent;

    private TaskState state;

    private PriorityLevel priorityLevel;
}
