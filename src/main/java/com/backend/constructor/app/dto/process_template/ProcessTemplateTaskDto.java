package com.backend.constructor.app.dto.process_template;

import com.backend.constructor.core.domain.enums.PriorityLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessTemplateTaskDto {
    private Long id;
    private Long stageId;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private Integer sortOrder;

    @Min(0)
    private Integer offsetDays;

    @Min(1)
    private Integer durationDays;

    private PriorityLevel priority;

    private String description;
}
