package com.backend.constructor.app.dto.account;

import com.backend.constructor.common.enums.ERole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDto {
    private Long id;
    private ERole name;
    private String description;
}
