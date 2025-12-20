package com.backend.constructor.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDto(
        @NotBlank(message = "{CST004}") String username
) {
}
