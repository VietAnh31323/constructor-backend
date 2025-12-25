package com.backend.constructor.app.dto.project;

import com.backend.constructor.core.domain.enums.ProjectState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "{ERROR_0001}")
    private String code;

    @NotBlank(message = "{ERROR_0001}")
    private String name;

    private String owner;

    private String address;

    private BigDecimal contractValue;

    private BigDecimal contractAdvance;

    private BigDecimal remainingAmount;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private ProjectState state;
}
