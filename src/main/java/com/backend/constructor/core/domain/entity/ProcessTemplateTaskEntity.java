package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "process_template_task")
public class ProcessTemplateTaskEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 1;

    @Column(name = "offset_days", nullable = false)
    @Builder.Default
    private Integer offsetDays = 0;

    @Column(name = "duration_days", nullable = false)
    @Builder.Default
    private Integer durationDays = 7;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 30, nullable = false)
    @Builder.Default
    private PriorityLevel priority = PriorityLevel.MEDIUM;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
