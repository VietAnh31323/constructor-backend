package com.backend.constructor.app.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordResponse {
    private Long accountId;
    private String username;
    private String temporaryPassword;
    private Boolean mustChangePassword;
}
