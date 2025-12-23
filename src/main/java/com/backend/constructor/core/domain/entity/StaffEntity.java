package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.enums.Position;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "staff")
public class StaffEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String email;

    @Enumerated(EnumType.STRING)
    private Position position;

    private String name;

    private String firstName;

    private String lastName;

    private String avatar;

    private LocalDate birthDate;

    private String address;

    private String phone;

    private String gender;

    private String description;

    private String genPassword;

    @Transient
    private Long accountId;

    public StaffEntity(Long id,
                       String code,
                       String name,
                       LocalDate birthDate,
                       String phone,
                       Position position,
                       Long accountId) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.birthDate = birthDate;
        this.phone = phone;
        this.position = position;
        this.accountId = accountId;
    }
}