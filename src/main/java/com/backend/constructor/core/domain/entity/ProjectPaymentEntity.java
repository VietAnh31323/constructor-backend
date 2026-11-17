package com.backend.constructor.core.domain.entity;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.converter.UploadDtoListConverter;
import com.backend.constructor.core.domain.enums.PaymentMethod;
import com.backend.constructor.core.domain.enums.ProjectPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_payment")
public class ProjectPaymentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_line_id")
    private Long projectLineId;

    @Column(name = "receipt_code", length = 50)
    private String receiptCode;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30, nullable = false)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private ProjectPaymentStatus status = ProjectPaymentStatus.CONFIRMED;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Convert(converter = UploadDtoListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    private List<UploadDto> attachments;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
