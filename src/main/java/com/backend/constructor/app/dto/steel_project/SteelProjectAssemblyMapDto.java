package com.backend.constructor.app.dto.steel_project;

import com.backend.constructor.app.dto.steel.SteelOutput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelProjectAssemblyMapDto {
    private Long id;

    @NotBlank
    private String assemblyName;

    @NotNull
    private Long sameQuantity;

    private Integer sortOrder;

    private String note;

    private BigDecimal totalLength;

    private BigDecimal totalWeight;

    @Valid
    private List<SteelOutput> steels;
}
