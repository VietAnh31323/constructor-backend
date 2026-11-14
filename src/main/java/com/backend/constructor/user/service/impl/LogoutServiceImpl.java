package com.backend.constructor.user.service.impl;

import com.backend.constructor.config.properties.RsaKeyProperties;
import com.backend.constructor.core.port.repository.TokenRepository;
import com.backend.constructor.user.security.jwt.TokenProvider;
import com.backend.constructor.user.service.TokenRevocationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutServiceImpl implements LogoutHandler {

    TokenRepository tokenRepository;
    TokenProvider tokenProvider;
    TokenRevocationService tokenRevocationService;
    RsaKeyProperties rsaKeyProperties;

    @Override
    @Transactional
    public void logout(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {
        final String accessToken = tokenProvider.resolveToken(request);
        final String refreshToken = extractRefreshTokenFromCookies(request);

        if (!ObjectUtils.isEmpty(refreshToken)) {
            tokenRepository.findByRefreshToken(refreshToken)
                    .ifPresent(entity -> {
                        entity.setRevoked(true);
                        tokenRepository.save(entity);
                    });
        }

        if (!ObjectUtils.isEmpty(accessToken)) {
            try {
                Jwt jwt = tokenProvider.decodeJwt(accessToken);
                tokenRevocationService.blacklistAccessToken(accessToken, jwt.getExpiresAt());
                tokenProvider.findAccountByTokenSubject(jwt)
                        .ifPresent(tokenRevocationService::revokeAllTokens);
            } catch (Exception e) {
                log.debug("Failed to decode access token during logout: {}", e.getMessage());
            }
        }

        Cookie clearCookie = new Cookie("refresh_token", "");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(rsaKeyProperties.cookieSecure());
        clearCookie.setPath("/");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
