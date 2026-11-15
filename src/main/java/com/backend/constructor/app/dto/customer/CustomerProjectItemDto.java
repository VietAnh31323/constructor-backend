package com.backend.constructor.app.dto.customer;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.enums.PaymentStatus;
import com.backend.constructor.core.domain.enums.ProjectState;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProjectItemDto {
    private Long id;
    private String code;
    private String name;
    private String address;
    private BigDecimal contractValue;
    private BigDecimal contractAdvance;
    private BigDecimal remainingAmount;
    private LocalDate signDate;
    private LocalDate deliveryDate;
    private BigDecimal progressPercent;
    private ProjectState state;
    private PaymentStatus paymentStatus;
    private CodeNameResponse manager;
}
