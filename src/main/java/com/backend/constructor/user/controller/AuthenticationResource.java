package com.backend.constructor.user.controller;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.user.dto.request.SignInRequest;
import com.backend.constructor.user.dto.request.SignUpRequest;
import com.backend.constructor.user.dto.response.AccountDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
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
}
