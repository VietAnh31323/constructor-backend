package com.backend.constructor.user.controller.impl;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.user.controller.AuthenticationResource;
import com.backend.constructor.user.dto.request.SignInRequest;
import com.backend.constructor.user.dto.request.SignUpRequest;
import com.backend.constructor.user.dto.response.AccountDto;
import com.backend.constructor.user.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthenticationResourceImpl implements AuthenticationResource {

    private final AuthenticationService authenticationService;
    private final LogoutHandler logoutHandler;

    @Override
    public IdResponse signUp(final SignUpRequest request) {
        return authenticationService.signUp(request);
    }

    @Override
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
}
