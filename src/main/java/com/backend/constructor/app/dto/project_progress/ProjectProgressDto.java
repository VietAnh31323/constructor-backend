package com.backend.constructor.app.dto.project_progress;

import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectProgressDto {
    private Long id;

    @Valid
    @NotNull
    private CodeNameResponse progress;

    @NotEmpty
    private List<TaskOutput> tasks;
}
