package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectFilterParam {
    private String search;
    private Long categoryId;
    private Long customerId;
    private ProjectState state;
}
