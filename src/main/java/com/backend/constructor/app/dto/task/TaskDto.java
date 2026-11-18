package com.backend.constructor.app.dto.task;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.TaskState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {
    private Long id;

    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private CodeNameResponse reviewer;

    private String description;

    private CodeNameResponse parent;

    private TaskState state;

    private PriorityLevel priorityLevel;

    @Valid
    private List<TaskStaffMapDto> taskStaffMaps;

    // 2.22: Trọng số tổng hợp tiến độ (mặc định 1)
    private BigDecimal weight;

    // 2.22 / 2.23 / 2.26: Thông tin chỉ đọc do hệ thống tính
    private BigDecimal progressPercent;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String returnReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private List<TaskState> allowedNextStates;
    private Boolean canReview;
    private Boolean hasChildren;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trim(this.description);
    }
}
