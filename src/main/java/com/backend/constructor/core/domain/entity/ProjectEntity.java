package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.validator.Unique;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import com.backend.constructor.core.domain.enums.BuildingType;
import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "customer_id")
    private Long customerId;

    private String owner;

    private String address;

    @Column(name = "contract_value", precision = 18, scale = 2)
    private BigDecimal contractValue;

    @Column(name = "contract_advance", precision = 18, scale = 2)
    private BigDecimal contractAdvance;

    @Column(name = "remaining_amount", precision = 18, scale = 2)
    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    @Column(name = "actual_handover_date")
    private LocalDate actualHandoverDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    private Long creatorId;

    private Long managerId;

    private Long supporterId;

    private BigDecimal progressPercent;

    private String description;

    private String note;

    @Enumerated(EnumType.STRING)
    private ProjectState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state")
    private ProjectState previousState;

    @Column(name = "state_reason")
    private String stateReason;

    @Column(name = "state_changed_at")
    private LocalDateTime stateChangedAt;

    @Convert(converter = UploadDtoListConverter.class)
    private List<UploadDto> contractFiles;

    @Convert(converter = UploadDtoListConverter.class)
    private List<UploadDto> sampleImages;

    @Convert(converter = UploadDtoListConverter.class)
    private List<UploadDto> projectImages;

    // 2.16: Thông tin kỹ thuật công trình
    @Enumerated(EnumType.STRING)
    @Column(name = "building_type", length = 100)
    private BuildingType buildingType;

    @Column(name = "land_area", precision = 12, scale = 2)
    private BigDecimal landArea;

    @Column(name = "construction_area", precision = 12, scale = 2)
    private BigDecimal constructionArea;

    @Column(name = "total_floor_area", precision = 12, scale = 2)
    private BigDecimal totalFloorArea;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    @Column(name = "construction_permit_no", length = 100)
    private String constructionPermitNo;

    @Column(name = "construction_permit_date")
    private LocalDate constructionPermitDate;

    @Column(name = "design_unit", length = 255)
    private String designUnit;
}
