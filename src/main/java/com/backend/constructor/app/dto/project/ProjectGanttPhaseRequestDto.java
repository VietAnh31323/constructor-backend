package com.backend.constructor.app.dto.project;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectGanttPhaseRequestDto {
    private Long progressId;
    private String newProgressName;
    private String description;
    // 2.21: ngày kế hoạch & trọng số giai đoạn
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private BigDecimal weight;

    public void trimData() {
        this.newProgressName = StringUtils.trimToNull(this.newProgressName);
        this.description = StringUtils.trimToNull(this.description);
    }
}
