package com.backend.constructor.app.dto.account;

import com.backend.constructor.common.enums.AccountStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountSimpleDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private AccountStatus status;
    private List<RoleDto> roles;
}
