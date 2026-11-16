package com.backend.constructor.app.dto.project;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.core.domain.enums.AddendumAdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractAddendumDto {
    private Long id;

    private Long contractId;

    private Long projectId;

    @NotBlank(message = "{ERROR_0001}")
    private String addendumNumber;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    @NotNull(message = "{ERROR_0001}")
    private AddendumAdjustmentType adjustmentType;

    private String adjustmentTypeLabel;

    private LocalDate signDate;

    private LocalDate effectiveDate;

    private Integer adjustedDays;

    private LocalDate newEndDate;

    private BigDecimal valueBeforeTax;

    private BigDecimal vatPercent;

    private BigDecimal vatAmount;

    private BigDecimal valueAfterTax;

    private String reason;

    private String content;

    private String note;

    private List<UploadDto> addendumFiles;
}
