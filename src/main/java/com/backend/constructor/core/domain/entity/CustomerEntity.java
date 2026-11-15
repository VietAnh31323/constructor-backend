package com.backend.constructor.core.domain.entity;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.validator.Unique;
import com.backend.constructor.core.domain.enums.ContactStatus;
import com.backend.constructor.core.domain.enums.CustomerPipelineStage;
import com.backend.constructor.core.domain.enums.CustomerType;
import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer")
public class CustomerEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Unique(err = "ERROR_0005")
    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type")
    private CustomerType customerType;

    @Unique(err = "CST034")
    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "representative_position")
    private String representativePosition;

    @Column(name = "id_card_number")
    private String idCardNumber;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_branch")
    private String bankBranch;

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    private String phone;

    private String email;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status")
    private ContactStatus contactStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_stage")
    private CustomerPipelineStage pipelineStage;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    private Boolean isPotential;

    private String description;

    private String note;
}
