package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.PaymentPlanStatus;
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
@Table(name = "project_line")
public class ProjectLineEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    private LocalDate paymentDate;

    private Long paymentNo;

    @Column(name = "payment_amount", precision = 18, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "condition_milestone", length = 500)
    private String conditionMilestone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private PaymentPlanStatus status = PaymentPlanStatus.PLANNED;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "last_due_alert_at")
    private LocalDateTime lastDueAlertAt;
}
