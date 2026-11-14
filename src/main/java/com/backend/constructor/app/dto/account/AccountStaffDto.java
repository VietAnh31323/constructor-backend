package com.backend.constructor.app.dto.account;

import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountStaffDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private AccountStatus status;
    private Boolean mustChangePassword;
    private Integer failedLoginAttempts;
    private Instant lockedUntil;
    private Instant createdAt;
    private List<RoleDto> roles;
    private StaffDto staff;
}
