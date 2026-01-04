package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.validator.Unique;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import com.backend.constructor.core.domain.enums.ProjectState;
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
@Table(name = "project")
public class ProjectEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Unique(err = "ERROR_0005")
    private String code;

    private String name;

    private String owner;

    private String address;

    private BigDecimal contractValue;

    private BigDecimal contractAdvance;

    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private Long creatorId;

    private Long managerId;

    private Long supporterId;

    private BigDecimal progressPercent;

    private String description;

    private String note;

    @Enumerated(EnumType.STRING)
    private ProjectState state;

    @Convert(converter = UploadDtoListConverter.class)
    private List<UploadDto> contractFiles;

    @Convert(converter = UploadDtoListConverter.class)
    private List<UploadDto> sampleImages;

    @Convert(converter = UploadDtoListConverter.class)
    private List<UploadDto> projectImages;
}