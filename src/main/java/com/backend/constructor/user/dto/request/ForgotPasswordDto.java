package com.backend.constructor.user.dto.request;

import com.backend.constructor.common.enums.ERole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordDto(
        @NotBlank(message = "{CST004}") String username,
        @NotNull(message = "{CST006}") ERole eRole
) {
}
