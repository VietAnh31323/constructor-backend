package com.backend.constructor.config.filter;

import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.user.security.jwt.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtAccessTokenFilter extends OncePerRequestFilter {

    final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest request,
                                    @NonNull final HttpServletResponse response,
                                    @NonNull final FilterChain filterChain)
            throws ServletException, IOException {

        final String token = tokenProvider.resolveToken(request);

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/password/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!tokenProvider.validateToken(token)) {
            SecurityContextHolder.clearContext();
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "Phiên đăng nhập không hợp lệ hoặc đã bị thu hồi!");
            return;
        }

        Jwt jwt = tokenProvider.decodeJwt(token);
        Optional<AccountEntity> accountOpt = tokenProvider.findAccountByTokenSubject(jwt);
        if (accountOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            if (account.getStatus() != AccountStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động!");
                return;
            }
            if (tokenProvider.isTokenRevokedForAccount(jwt, account)) {
                SecurityContextHolder.clearContext();
                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_REVOKED", "Phiên đăng nhập đã hết hiệu lực, vui lòng đăng nhập lại!");
                return;
            }
            if (Boolean.TRUE.equals(account.getMustChangePassword()) && !isAllowedDuringForcedPasswordChange(uri, request.getMethod())) {
                SecurityContextHolder.clearContext();
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "MUST_CHANGE_PASSWORD", "Vui lòng đổi mật khẩu trong lần đăng nhập đầu tiên trước khi sử dụng hệ thống!");
                return;
            }
        }

        Authentication authentication = tokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[JwtAccessTokenFilter:doFilterInternal] Authentication set to SecurityContextHolder : {}", authentication.getName());

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedDuringForcedPasswordChange(String uri, String method) {
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/logout")) {
            return true;
        }
        if (uri.equals("/api/v1/account/change-password")) {
            return true;
        }
        if (uri.equals("/api/v1/account/permissions")) {
            return true;
        }
        return uri.equals("/api/v1/account") && "GET".equalsIgnoreCase(method);
    }

    private void writeJsonError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = """
                {"message":"%s","errors":[{"code":"%s","message":"%s"}]}""".formatted(message, code, message);
        response.getWriter().write(body);
    }
}
