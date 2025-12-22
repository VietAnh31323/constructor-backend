package com.backend.constructor.app.dto.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountOutput {
    private Long id;
    private String username;
    private List<RoleDto> roles;
    private Long staffCount;
}
