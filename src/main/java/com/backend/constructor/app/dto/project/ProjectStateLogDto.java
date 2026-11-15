package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStateLogDto {
    private Long id;
    private Long projectId;
    private ProjectState fromState;
    private String fromStateLabel;
    private ProjectState toState;
    private String toStateLabel;
    private String reason;
    private Long changedByStaffId;
    private CodeNameResponse changedByStaff;
    private String changedByUsername;
    private Instant createdAt;
}
