package com.backend.constructor.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordDto(
        @NotBlank(message = "{CST010}") String oldPassword,
        @NotBlank(message = "{CST012}") String newPassword,
        @NotBlank(message = "{CST013}") String confirmPassword
) {
}
