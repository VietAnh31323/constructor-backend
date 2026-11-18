package com.backend.constructor.app.dto.project_progress;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhaseReorderDto {
    @NotEmpty(message = "{ERROR_0001}")
    private List<Long> orderedIds;
}
