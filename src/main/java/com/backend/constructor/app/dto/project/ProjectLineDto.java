package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.PaymentPlanStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectLineDto {
    private Long id;

    @NotNull(message = "{ERROR_0001}")
    private LocalDate paymentDate;

    @NotNull(message = "{ERROR_0001}")
    private Long paymentNo;

    @Min(0)
    @Digits(integer = 16, fraction = 2)
    private BigDecimal paymentAmount;

    @Min(0)
    @DecimalMax("100.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal percentage;

    private String conditionMilestone;

    private PaymentPlanStatus status;

    private String statusLabel;

    private String note;

    // Computed read-only fields showing actual collected amount against this installment
    private BigDecimal collectedAmount;

    private BigDecimal remainingLineAmount;
}
