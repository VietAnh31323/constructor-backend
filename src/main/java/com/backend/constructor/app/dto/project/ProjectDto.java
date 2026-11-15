package com.backend.constructor.app.dto.project;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.CustomerType;
import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    private Long id;

    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private Long customerId;

    private CodeNameResponse customer;

    private CustomerType customerType;

    private String customerPhone;

    private String customerTaxCode;

    private String owner;

    private String address;

    @Min(0)
    @Digits(integer = 16, fraction = 2)
    private BigDecimal contractValue;

    @Min(0)
    @Digits(integer = 16, fraction = 2)
    private BigDecimal contractAdvance;

    @Digits(integer = 16, fraction = 2)
    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private LocalDate actualHandoverDate;

    private LocalDate warrantyEndDate;

    private BigDecimal progressPercent;

    private CodeNameResponse creator;

    private CodeNameResponse manager;

    private CodeNameResponse supporter;

    private String description;

    private String note;

    private ProjectState state;

    private ProjectState previousState;

    private String stateReason;

    private LocalDateTime stateChangedAt;

    private List<ProjectState> allowedNextStates;

    private Integer stateLogCount;

    private List<UploadDto> contractFiles;

    private List<UploadDto> sampleImages;

    private List<UploadDto> projectImages;

    @Valid
    @NotEmpty(message = "{ERROR_0001}")
    private List<ProjectCategoryMapDto> projectCategoryMaps;

    @Valid
    private List<ProjectLineDto> projectLines;

    public void trimData() {
        this.code = StringUtils.trimToNull(this.code);
        this.name = StringUtils.trim(this.name);
        this.owner = StringUtils.trimToNull(this.owner);
        this.address = StringUtils.trimToNull(this.address);
        this.note = StringUtils.trimToNull(this.note);
        this.stateReason = StringUtils.trimToNull(this.stateReason);
    }
}
