package com.backend.constructor.app.dto.project;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectGanttPhaseRequestDto {
    private Long progressId;
    private String newProgressName;
    private String description;

    public void trimData() {
        this.newProgressName = StringUtils.trimToNull(this.newProgressName);
        this.description = StringUtils.trimToNull(this.description);
    }
}
