package com.backend.constructor.user.service;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.user.dto.request.SignInRequest;
import com.backend.constructor.user.dto.request.SignUpRequest;
import com.backend.constructor.user.dto.response.AccountDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public interface AuthenticationService {
    IdResponse signUp(SignUpRequest request);

    AccountDto signIn(SignInRequest request, HttpServletResponse response);

    AccountDto accessTokenByRefreshToken(HttpServletRequest req);

}
