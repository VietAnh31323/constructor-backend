package com.backend.constructor.user.dto.request;

import com.backend.constructor.common.enums.ERole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SignInRequest(
        @NotBlank(message = "{CST004}") String username,
        @NotBlank(message = "{CST005}") String password,
        @NotNull(message = "{CST006}") ERole eRole
) {
}
