package com.backend.constructor.app.dto.project;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.PaymentStatus;
import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectOutput {
    private Long id;

    private String code;

    private String name;

    private String owner;

    private CodeNameResponse manager;

    private BigDecimal contractValue;

    private BigDecimal contractAdvance;

    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private ProjectState state;

    private PaymentStatus paymentStatus;

    private BigDecimal progressPercent;
}
