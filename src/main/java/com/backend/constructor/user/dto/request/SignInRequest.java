package com.backend.constructor.user.dto.request;

import com.backend.constructor.common.enums.ERole;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank(message = "Username không được để trống") String username,
        @NotBlank(message = "Password không được để trống") String password,
        ERole eRole
) {
}
