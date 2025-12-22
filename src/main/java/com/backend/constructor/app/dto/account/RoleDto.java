package com.backend.constructor.app.dto.account;

import com.backend.constructor.common.enums.ERole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDto {
    private Long id;
    private ERole name;
}
