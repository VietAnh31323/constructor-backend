package com.backend.constructor.app.dto.project;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.core.domain.enums.PaymentMethod;
import com.backend.constructor.core.domain.enums.ProjectPaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPaymentDto {
    private Long id;

    private Long projectId;

    private Long projectLineId;

    private Long paymentPlanNo;

    private String paymentPlanMilestone;

    private String receiptCode;

    @NotNull(message = "{ERROR_0001}")
    private LocalDate paymentDate;

    @NotNull(message = "{ERROR_0001}")
    @DecimalMin(value = "0.01", message = "Số tiền thu phải lớn hơn 0")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private String paymentMethodLabel;

    private ProjectPaymentStatus status;

    private String statusLabel;

    private String payerName;

    private List<UploadDto> attachments;

    private String note;

    private Instant createdAt;

    private String createdBy;
}
