package com.backend.constructor.app.dto.account;

import com.backend.constructor.app.dto.staff.StaffDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountStaffDto {
    private Long id; // id của account
    private String username;
    private List<RoleDto> roles;
    private StaffDto staff;
}
