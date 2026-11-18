package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import com.backend.constructor.core.domain.enums.SiteReportPhase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task_site_report")
public class TaskSiteReportEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "project_id")
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_phase", length = 20, nullable = false)
    @Builder.Default
    private SiteReportPhase reportPhase = SiteReportPhase.DURING;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Convert(converter = UploadDtoListConverter.class)
    @Column(name = "images", columnDefinition = "TEXT")
    private List<UploadDto> images;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "location_accuracy", precision = 10, scale = 2)
    private BigDecimal locationAccuracy;

    @Column(name = "location_address", length = 500)
    private String locationAddress;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    @Column(name = "reporter_staff_id")
    private Long reporterStaffId;

    @Column(name = "reporter_name", length = 255)
    private String reporterName;
}
