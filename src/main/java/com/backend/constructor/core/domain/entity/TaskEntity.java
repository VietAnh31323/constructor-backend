package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.validator.Unique;
import com.backend.constructor.core.domain.enums.PriorityLevel;
import com.backend.constructor.core.domain.enums.TaskState;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

}