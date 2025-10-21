package com.backend.constructor.user.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Entity
@Builder
@Table(
        name = "accounts",
        indexes = {
                @Index(
                        columnList = "username",
                        unique = true
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String username;

    String passwordHash;

    String avatar;

    String displayName;

    @Column(unique = true)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    AuthProvider authProvider = AuthProvider.EMAIL_AND_PASSWORD;
}