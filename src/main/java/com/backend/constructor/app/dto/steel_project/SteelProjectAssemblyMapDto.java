package com.backend.constructor.app.dto.steel_project;

import com.backend.constructor.app.dto.steel.SteelOutput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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

    @Valid
    @NotEmpty
    private List<SteelOutput> steels;
}
