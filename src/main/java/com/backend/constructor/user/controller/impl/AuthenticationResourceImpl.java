package com.backend.constructor.user.controller.impl;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.Message;
import com.backend.constructor.user.controller.AuthenticationResource;
import com.backend.constructor.user.dto.request.*;
import com.backend.constructor.user.dto.response.AccountDto;
import com.backend.constructor.user.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class AuthenticationResourceImpl implements AuthenticationResource {

    private final AuthenticationService authenticationService;
    private final LogoutHandler logoutHandler;

    @Override
    public IdResponse signUp(final SignUpRequest request) {
        return authenticationService.signUp(request);
    }

    @Override
    @Message("SUCCESS_001")
    public AccountDto signIn(final SignInRequest request, final HttpServletResponse response) {
        return authenticationService.signIn(request, response);
    }

    @Override
    public AccountDto getAccessTokenByRefreshToken(final HttpServletRequest request) {
        return authenticationService.accessTokenByRefreshToken(request);
    }

    @Override
    public Boolean logout(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return Boolean.TRUE;
    }

    @Override
    @Message("SUCCESS_002")
    public void forgotPassword(@RequestBody @Valid ForgotPasswordDto input) {
        authenticationService.forgotPassword(input.username());
    }

    @Override
    @Message("SUCCESS_004")
    public VerifyOtpDto verifyOtp(@RequestBody @Valid VerifyOtpDto input) {
        return authenticationService.verifyOtp(input);
    }

    @Override
    @Message("SUCCESS_003")
    public void resetPassword(@RequestBody @Valid ResetPasswordDto input, HttpServletRequest req) {
        authenticationService.resetPassword(input, req);
    }
}
