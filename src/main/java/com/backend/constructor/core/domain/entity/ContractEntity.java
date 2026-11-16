package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import com.backend.constructor.core.domain.enums.ContractStatus;
import com.backend.constructor.core.domain.enums.ContractType;
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
@Table(name = "contract")
public class ContractEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "contract_number", nullable = false)
    private String contractNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status;

    @Column(name = "sign_date")
    private LocalDate signDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "value_before_tax", precision = 18, scale = 2)
    private BigDecimal valueBeforeTax;

    @Column(name = "vat_percent", precision = 5, scale = 2)
    private BigDecimal vatPercent;

    @Column(name = "vat_amount", precision = 18, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "value_after_tax", precision = 18, scale = 2)
    private BigDecimal valueAfterTax;

    @Column(name = "advance_amount", precision = 18, scale = 2)
    private BigDecimal advanceAmount;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "warranty_percent", precision = 5, scale = 2)
    private BigDecimal warrantyPercent;

    @Column(name = "warranty_amount", precision = 18, scale = 2)
    private BigDecimal warrantyAmount;

    @Column(name = "terms")
    private String terms;

    @Column(name = "note")
    private String note;

    @Convert(converter = UploadDtoListConverter.class)
    @Column(name = "contract_files")
    private List<UploadDto> contractFiles;
}
