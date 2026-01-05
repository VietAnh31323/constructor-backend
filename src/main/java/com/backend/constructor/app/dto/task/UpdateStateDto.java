package com.backend.constructor.app.dto.task;

import com.backend.constructor.core.domain.enums.TaskState;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStateDto {
    @NotNull
    private Long id;
    @NotNull
    private TaskState state;
}
