package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.CareChannel;
import com.backend.constructor.core.domain.enums.CareOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_care_log")
public class CustomerCareLogEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "staff_id")
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "care_channel", nullable = false)
    private CareChannel careChannel;

    @Column(name = "care_time", nullable = false)
    private LocalDateTime careTime;

    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private CareOutcome outcome;

    @Column(name = "next_action_date")
    private LocalDateTime nextActionDate;

    @Column(name = "next_action_note")
    private String nextActionNote;
}
