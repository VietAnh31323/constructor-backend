package com.backend.constructor.app.dto.project;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    private Long id;

    @NotNull(message = "{ERROR_0001}")
    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private String owner;

    private String address;

    @NotNull(message = "{ERROR_0001}")
    @Min(0)
    private BigDecimal contractValue;

    @Min(0)
    private BigDecimal contractAdvance;

    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private CodeNameResponse creator;

    private CodeNameResponse manager;

    private CodeNameResponse supporter;

    private String description;

    private String note;

    private ProjectState state;

    private List<UploadDto> contractFiles;

    private List<UploadDto> sampleImages;

    private List<UploadDto> projectImages;

    @Valid
    @NotEmpty(message = "{ERROR_0001}")
    private List<ProjectCategoryMapDto> projectCategoryMaps;

    @Valid
    private List<ProjectLineDto> projectLines;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.note = StringUtils.trim(this.note);
    }
}
