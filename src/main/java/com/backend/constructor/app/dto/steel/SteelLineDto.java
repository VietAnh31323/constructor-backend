package com.backend.constructor.app.dto.steel;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelLineDto {
    private Long id;

    @NotNull
    private String paramName;

    @NotNull
    private BigDecimal value;
}
