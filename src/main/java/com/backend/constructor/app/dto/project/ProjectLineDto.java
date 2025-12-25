package com.backend.constructor.app.dto.project;

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

    @NotNull(message = "{ERROR_0001}")
    private BigDecimal paymentAmount;
}
