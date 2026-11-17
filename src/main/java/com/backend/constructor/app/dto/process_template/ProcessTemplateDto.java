package com.backend.constructor.app.dto.process_template;

import com.backend.constructor.core.domain.enums.BuildingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessTemplateDto {
    private Long id;

    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    @NotNull(message = "{ERROR_0001}")
    private BuildingType buildingType;

    private String buildingTypeLabel;

    private String description;

    private Boolean isActive;

    private Boolean isDefault;

    private Integer stageCount;

    private Integer taskCount;

    @Valid
    @Builder.Default
    private List<ProcessTemplateStageDto> stages = new ArrayList<>();
}
