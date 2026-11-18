package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.TaskState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectGanttTaskRequestDto {
    private Long projectProgressId;
    private Long parentTaskId;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    @NotNull(message = "{ERROR_0001}")
    private LocalDate startDate;

    @NotNull(message = "{ERROR_0001}")
    private LocalDate endDate;

    private PriorityLevel priorityLevel;

    /** Chỉ chấp nhận NOT_STARTED hoặc IN_PROGRESS khi tạo mới (2.23). */
    private TaskState state;

    /** @deprecated 2.22: % tiến độ do hệ thống tự tính, giá trị này bị bỏ qua. */
    @Deprecated
    private BigDecimal progressPercent;

    /** 2.22: Trọng số công việc khi tổng hợp tiến độ (mặc định 1). */
    private BigDecimal weight;

    private Long reviewerId;
    private List<Long> staffIds;
    private String description;

    public void trimData() {
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trimToNull(this.description);
    }
}
