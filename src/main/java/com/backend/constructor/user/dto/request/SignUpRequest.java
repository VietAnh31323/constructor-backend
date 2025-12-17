package com.backend.constructor.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SignUpRequest(
        @NotBlank
        @Email(message = "Username phải là địa chỉ email hợp lệ")
        String username,
        @NotBlank
        String password,
        String firstName,
        String lastName) {
}
