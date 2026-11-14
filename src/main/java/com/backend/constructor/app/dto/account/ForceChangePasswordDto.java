package com.backend.constructor.app.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForceChangePasswordDto {
    @NotNull(message = "ERROR_0001")
    private Boolean mustChangePassword;
}
