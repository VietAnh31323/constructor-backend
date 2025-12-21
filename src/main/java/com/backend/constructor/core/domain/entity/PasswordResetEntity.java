package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "password_reset")
public class PasswordResetEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;

    private String otpHash;

    private Instant expiresAt;

    private Boolean used;
}
