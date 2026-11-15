package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_state_log")
public class ProjectStateLogEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private ProjectState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false)
    private ProjectState toState;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_by_staff_id")
    private Long changedByStaffId;

    @Column(name = "changed_by_username")
    private String changedByUsername;
}
