package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStateTransitionDto {
    @NotNull(message = "{CST019}")
    private ProjectState toState;

    private String reason;

    private LocalDate actualHandoverDate;

    private LocalDate warrantyEndDate;

    public void trimData() {
        this.reason = StringUtils.trimToNull(this.reason);
    }
}
