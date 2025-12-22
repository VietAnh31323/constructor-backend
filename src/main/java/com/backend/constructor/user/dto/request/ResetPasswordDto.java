package com.backend.constructor.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDto(
        @NotBlank(message = "{CST008}") String newPassword
) {
}
