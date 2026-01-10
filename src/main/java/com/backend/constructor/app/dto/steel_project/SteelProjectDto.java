package com.backend.constructor.app.dto.steel_project;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SteelProjectDto {
    private Long id;

    private String code;

    @NotNull
    private String name;

    private String owner;

    private String address;

    private LocalDate signDate;

    private LocalDate deliveryDate;

    private String description;

    private List<SteelProjectAssemblyMapDto> steelProjectAssemblyMaps;

    public void trimData() {
        this.code = StringUtils.trim(this.code);
        this.name = StringUtils.trim(this.name);
        this.description = StringUtils.trim(this.description);
    }
}
