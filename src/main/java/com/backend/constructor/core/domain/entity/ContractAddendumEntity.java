package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import com.backend.constructor.core.domain.enums.AddendumAdjustmentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contract_addendum")
public class ContractAddendumEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "addendum_number", nullable = false)
    private String addendumNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AddendumAdjustmentType adjustmentType;

    @Column(name = "sign_date")
    private LocalDate signDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "adjusted_days")
    private Integer adjustedDays;

    @Column(name = "new_end_date")
    private LocalDate newEndDate;

    @Column(name = "value_before_tax", precision = 18, scale = 2)
    private BigDecimal valueBeforeTax;

    @Column(name = "vat_percent", precision = 5, scale = 2)
    private BigDecimal vatPercent;

    @Column(name = "vat_amount", precision = 18, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "value_after_tax", precision = 18, scale = 2)
    private BigDecimal valueAfterTax;

    @Column(name = "reason")
    private String reason;

    @Column(name = "content")
    private String content;

    @Column(name = "note")
    private String note;

    @Convert(converter = UploadDtoListConverter.class)
    @Column(name = "addendum_files")
    private List<UploadDto> addendumFiles;
}
