package com.backend.constructor.app.dto.project;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.core.domain.enums.ContractStatus;
import com.backend.constructor.core.domain.enums.ContractType;
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
public class ContractDto {
    private Long id;

    private Long projectId;

    @NotBlank(message = "{ERROR_0001}")
    private String contractNumber;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    @NotNull(message = "{ERROR_0001}")
    private ContractType contractType;

    private String contractTypeLabel;

    @NotNull(message = "{ERROR_0001}")
    private ContractStatus status;

    private String statusLabel;

    private LocalDate signDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal valueBeforeTax;

    private BigDecimal vatPercent;

    private BigDecimal vatAmount;

    private BigDecimal valueAfterTax;

    private BigDecimal advanceAmount;

    private Integer warrantyMonths;

    private BigDecimal warrantyPercent;

    private BigDecimal warrantyAmount;

    private String terms;

    private String note;

    private List<UploadDto> contractFiles;

    private List<ContractAddendumDto> addendums;

    // Tổng giá trị sau khi cộng trừ các phụ lục hợp đồng
    private BigDecimal totalAdjustedValue;

    private BigDecimal totalAddendumCount;
}
