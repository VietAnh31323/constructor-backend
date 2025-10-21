package com.backend.constructor.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
        @NotBlank
        @Email(message = "Username phải là địa chỉ email hợp lệ")
        String username,
        @NotBlank
        String password,
        @NotBlank
        String displayName
) {
}
