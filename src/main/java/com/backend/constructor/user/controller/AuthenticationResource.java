package com.backend.constructor.user.controller;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.user.dto.request.*;
import com.backend.constructor.user.dto.response.AccountDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthenticationResource {
    @PostMapping(value = "/sign-up")
    IdResponse signUp(@Valid @RequestBody SignUpRequest request);

    @PostMapping("/sign-in")
    AccountDto signIn(@Valid @RequestBody SignInRequest request, final HttpServletResponse response);

    @PreAuthorize("hasAnyAuthority('SCOPE_REFRESH_TOKEN')")
    @PostMapping("/refresh-token")
    AccountDto getAccessTokenByRefreshToken(HttpServletRequest req);

    @PostMapping("/logout")
    Boolean logout(HttpServletRequest request, HttpServletResponse response);

    @PostMapping("/password/forgot")
    void forgotPassword(ForgotPasswordDto input);

    @PostMapping("/password/verify-otp")
    VerifyOtpDto verifyOtp(VerifyOtpDto input);

    @PostMapping("/password/reset")
    void resetPassword(ResetPasswordDto input, final HttpServletRequest req);
}
