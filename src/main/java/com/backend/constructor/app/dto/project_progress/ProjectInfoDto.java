package com.backend.constructor.app.dto.project_progress;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectInfoDto {
    private Long id;

    private String code;

    private String name;

    private String owner;

    private String address;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private CodeNameResponse creator;

    private CodeNameResponse manager;

    private CodeNameResponse supporter;

    private String description;

    private String note;

    private ProjectState state;

    private BigDecimal progressPercent;

    @Valid
    @NotEmpty
    private List<ProjectProgressDto> projectProgress;
}
