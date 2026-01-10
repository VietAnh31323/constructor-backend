package com.backend.constructor.app.dto.assembly;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssemblyDto {
    private Long id;
    private String code;
    @NotNull
    private String name;
}
