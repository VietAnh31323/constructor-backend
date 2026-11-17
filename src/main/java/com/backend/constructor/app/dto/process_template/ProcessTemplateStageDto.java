package com.backend.constructor.app.dto.process_template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessTemplateStageDto {
    private Long id;
    private Long templateId;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private Integer sortOrder;

    @Min(1)
    private Integer durationDays;

    private String description;

    @Valid
    @Builder.Default
    private List<ProcessTemplateTaskDto> tasks = new ArrayList<>();
}
