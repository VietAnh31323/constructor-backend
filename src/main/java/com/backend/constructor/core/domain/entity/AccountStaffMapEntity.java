package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account_staff_map")
public class AccountStaffMapEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long staffId;

    private Long accountId;
}