package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "process_template_stage")
public class ProcessTemplateStageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 1;

    @Column(name = "duration_days", nullable = false)
    @Builder.Default
    private Integer durationDays = 14;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
