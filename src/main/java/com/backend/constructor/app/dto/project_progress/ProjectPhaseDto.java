package com.backend.constructor.app.dto.project_progress;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 2.21: Giai đoạn tiến độ của dự án (sửa / xóa / sắp xếp, ngày kế hoạch & thực tế, trọng số).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPhaseDto {
    private Long projectProgressId;
    private Long projectId;

    /** Giai đoạn danh mục (progress). Có thể đổi sang giai đoạn khác hoặc nhập tên mới. */
    private Long progressId;
    private String progressCode;
    private String progressName;
    private String newProgressName;

    private Integer sortOrder;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;

    /** Ngày thực tế: hệ thống tự tổng hợp từ công việc (chỉ đọc). */
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    @DecimalMin(value = "0.01", message = "Trọng số phải lớn hơn 0")
    @DecimalMax(value = "1000")
    private BigDecimal weight;

    /** % tiến độ: hệ thống tự tính (chỉ đọc). */
    private BigDecimal progressPercent;
    private String note;

    private Integer taskCount;
    private Integer completedTaskCount;
    private Integer pendingReviewTaskCount;
    private Integer overdueTaskCount;
    private Long delayDays;
    private String scheduleStatus;
}
