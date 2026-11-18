package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.validator.Unique;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.TaskState;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task")
public class TaskEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Unique(err = "ERROR_0005")
    private String code;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal progressPercent;

    private Long reviewerId;

    private Long parentId;

    private String description;

    @Enumerated(EnumType.STRING)
    private PriorityLevel priorityLevel;

    @Enumerated(EnumType.STRING)
    private TaskState state;

    // 2.22: Trọng số dùng khi tự động tổng hợp % tiến độ lên việc cha / giai đoạn / dự án
    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;

    // 2.21 + 2.26: Ngày thực tế (so sánh với startDate / endDate kế hoạch)
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    // 2.23: Thông tin trình duyệt / duyệt / trả lại
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;

    // 2.26: Lần cuối gửi cảnh báo trễ hạn (tránh gửi trùng trong ngày)
    @Column(name = "overdue_notified_at")
    private LocalDateTime overdueNotifiedAt;
}