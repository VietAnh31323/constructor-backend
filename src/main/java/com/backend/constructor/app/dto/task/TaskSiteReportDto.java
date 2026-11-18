package com.backend.constructor.app.dto.task;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.core.domain.enums.SiteReportPhase;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskSiteReportDto {
    private Long id;
    private Long taskId;
    private Long projectId;
    private SiteReportPhase reportPhase;
    private String reportPhaseLabel;
    private String note;
    private List<UploadDto> images;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal longitude;

    private BigDecimal locationAccuracy;
    private String locationAddress;
    private LocalDateTime capturedAt;
    private Long reporterStaffId;
    private String reporterName;
    private Instant createdAt;
    private Boolean canDelete;
}
