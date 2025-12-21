package com.backend.constructor.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record VerifyOtpDto(
        @NotBlank(message = "{CST004}") String username,
        @NotBlank(message = "{CST008}") String otp,
        String token,
        String authScheme
) {
}
