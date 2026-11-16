package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.ProjectMemberRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_member")
public class ProjectMemberEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private ProjectMemberRole memberRole;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    private String note;
}
