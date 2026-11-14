package com.backend.constructor.user.service.impl;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.common.enums.TokenType;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.MailService;
import com.backend.constructor.config.languages.Translator;
import com.backend.constructor.config.properties.RsaKeyProperties;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.AuthScheme;
import com.backend.constructor.core.port.mapper.AccountMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.core.service.AuditLogService;
import com.backend.constructor.core.service.HelperService;
import com.backend.constructor.user.dto.request.*;
import com.backend.constructor.user.dto.response.AccountDto;
import com.backend.constructor.user.security.jwt.TokenProvider;
import com.backend.constructor.user.service.AuthenticationService;
import com.backend.constructor.user.service.LoginAttemptService;
import com.backend.constructor.user.service.TokenRevocationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
    PasswordEncoder passwordEncoder;
    AuthenticationManagerBuilder authenticationManagerBuilder;
    TokenProvider tokenProvider;
    Translator translator;
    RsaKeyProperties rsaKeyProperties;

    AccountRepository accountRepository;
    RoleRepository roleRepository;
    TokenRepository tokenRepository;
    PermissionRepository permissionRepository;

    AccountMapper accountMapper;
    AccountRoleMapRepository accountRoleMapRepository;
    MailService mailService;
    PasswordResetRepository passwordResetRepository;
    LoginAttemptService loginAttemptService;
    TokenRevocationService tokenRevocationService;
    AuditLogService auditLogService;

    @Override
    @Transactional
    public IdResponse signUp(SignUpRequest request) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new BusinessException(String.valueOf(HttpStatus.BAD_REQUEST.value()), translator.toLocale("CST014", request.username()));
        }
        final var entity = accountMapper.toEntity(request);
        RoleEntity role = roleRepository.getByName(ERole.STAFF)
                .orElse(RoleEntity.builder().name(ERole.STAFF).description("Nhân viên").build());
        entity.setPassword(passwordEncoder.encode(request.password()));
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setMustChangePassword(true);
        entity.setFailedLoginAttempts(0);
        accountRepository.save(entity);
        saveAccountRoleMap(entity.getId(), role);
        mailService.sendPasswordEmail(entity.getUsername(), request.password(), role.getName().getValue());
        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public AccountDto signIn(SignInRequest request, HttpServletResponse response) {
        String trimmedUsername = request.username().trim();
        String clientIp = extractClientIp();

        loginAttemptService.checkAllowed(trimmedUsername, clientIp);

        Optional<AccountEntity> existingAccountOpt = accountRepository.findAnyByUsername(trimmedUsername);
        if (existingAccountOpt.isPresent()) {
            AccountEntity existing = existingAccountOpt.get();
            if (existing.getStatus() != AccountStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, translator.toLocale("CST001"));
            }
        }

        final var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                trimmedUsername,
                request.password().trim()
        );

        final Authentication authentication;
        try {
            authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailedAttempt(trimmedUsername, clientIp);
            throw ex;
        }

        final var accountOpt = accountRepository.findOptionalByUsernameAndRole(authentication.getName(), request.eRole());
        if (accountOpt.isEmpty()) {
            loginAttemptService.recordFailedAttempt(trimmedUsername, clientIp);
            throw new BadCredentialsException("Invalid role for account");
        }
        final var account = accountOpt.get();

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, translator.toLocale("CST001"));
        }

        loginAttemptService.resetAttempts(account, clientIp);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        final var jwtAccessToken = tokenProvider.createToken(authentication);
        final var jwtRefreshToken = tokenProvider.createRefreshToken(authentication);

        AccountDto dto = getAccountDto(response, account, jwtAccessToken, jwtRefreshToken);

        auditLogService.logExplicit(
                "LOGIN",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(account.getId()),
                null,
                Map.of("role", request.eRole().name()),
                "Đăng nhập thành công với vai trò " + request.eRole().getValue(),
                account.getUsername()
        );

        return dto;
    }

    @Override
    @Transactional
    public AccountDto accessTokenByRefreshToken(HttpServletRequest req) {
        final var refreshToken = tokenProvider.resolveToken(req);

        if (ObjectUtils.isEmpty(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please verify your token");
        }
        var refreshTokenEntity = tokenRepository.findByRefreshToken(refreshToken)
                .filter(tokens -> !Boolean.TRUE.equals(tokens.getRevoked()))
                .orElseThrow(() -> BusinessException.exception("CST009"));

        final var account = accountRepository.getAccountById(refreshTokenEntity.getAccountId());
        if (account.getStatus() != AccountStatus.ACTIVE) {
            tokenRevocationService.revokeAllTokens(account);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, translator.toLocale("CST001"));
        }

        updateRevokedRefreshToken(account);

        final var authentication = tokenProvider.getAuthentication(account);
        final var jwtAccessToken = tokenProvider.createToken(authentication);
        final var jwtRefreshToken = tokenProvider.createRefreshToken(authentication);
        saveRefreshToken(account, jwtRefreshToken);

        AccountDto dto = accountMapper.toDto(jwtAccessToken, jwtRefreshToken);
        enrichAccountDto(dto, account);
        return dto;
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDto input) {
        Optional<AccountEntity> account = accountRepository.findOptionalByUsernameAndRole(input.username(), input.eRole());
        if (account.isEmpty()) {
            return;
        }
        AccountEntity accountEntity = account.get();
        if (accountEntity.getStatus() != AccountStatus.ACTIVE) {
            return;
        }
        String generateOtp = HelperService.generateOtp();
        final var now = Instant.now();
        Instant expiresAt = now.plus(90, ChronoUnit.SECONDS);
        PasswordResetEntity passwordResetEntity = PasswordResetEntity.builder()
                .accountId(accountEntity.getId())
                .expiresAt(expiresAt)
                .otpHash(passwordEncoder.encode(generateOtp))
                .used(false)
                .build();
        passwordResetRepository.save(passwordResetEntity);
        mailService.sendOtp(accountEntity.getUsername(), generateOtp);
    }

    @Override
    @Transactional
    public VerifyOtpDto verifyOtp(VerifyOtpDto input) {
        Optional<AccountEntity> optionalAccount = accountRepository.findByUsername(input.username());
        if (optionalAccount.isEmpty()) {
            throw BusinessException.exception("CST000");
        }
        AccountEntity accountEntity = optionalAccount.get();
        PasswordResetEntity otpValid = passwordResetRepository.getByAccountIdAndOtpValid(accountEntity.getId());
        if (!passwordEncoder.matches(input.otp(), otpValid.getOtpHash())) {
            throw BusinessException.exception("CST011");
        }
        otpValid.setUsed(true);
        passwordResetRepository.save(otpValid);
        Jwt jwt = tokenProvider.createResetToken(accountEntity.getUsername());
        saveResetToken(accountEntity, jwt);
        return VerifyOtpDto.builder()
                .username(accountEntity.getUsername())
                .otp(input.otp())
                .token(jwt.getTokenValue())
                .authScheme(AuthScheme.BEARER.toString())
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordDto input, HttpServletRequest req) {
        final var resetToken = tokenProvider.resolveToken(req);
        if (ObjectUtils.isEmpty(resetToken)) {
            throw BusinessException.exception("CST009");
        }
        var resetTokenEntity = tokenRepository.findByResetToken(resetToken);
        if (!tokenProvider.validateToken(resetTokenEntity.getToken())) {
            throw BusinessException.exception("CST009");
        }
        resetTokenEntity.setRevoked(true);
        tokenRepository.save(resetTokenEntity);

        AccountEntity accountEntity = accountRepository.getAccountById(resetTokenEntity.getAccountId());
        accountEntity.setPassword(passwordEncoder.encode(input.newPassword()));
        accountEntity.setMustChangePassword(false);
        accountEntity.setFailedLoginAttempts(0);
        accountEntity.setLockedUntil(null);
        accountRepository.save(accountEntity);

        tokenRevocationService.revokeAllTokens(accountEntity);

        auditLogService.logExplicit(
                "RESET_PASSWORD",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(accountEntity.getId()),
                null,
                Map.of("mustChangePassword", false, "tokensRevoked", true),
                "Đặt lại mật khẩu qua OTP cho tài khoản " + accountEntity.getUsername(),
                accountEntity.getUsername()
        );
    }

    private AccountDto getAccountDto(HttpServletResponse response, AccountEntity account, Jwt jwtAccessToken, Jwt jwtRefreshToken) {
        updateRevokedRefreshToken(account);
        saveRefreshToken(account, jwtRefreshToken);
        createRefreshTokenCookie(response, jwtRefreshToken);

        AccountDto dto = accountMapper.toDto(jwtAccessToken, jwtRefreshToken);
        enrichAccountDto(dto, account);
        return dto;
    }

    private void enrichAccountDto(AccountDto dto, AccountEntity account) {
        dto.setUserId(account.getId());
        dto.setFirstName(account.getFirstName());
        dto.setLastName(account.getLastName());
        dto.setMustChangePassword(Boolean.TRUE.equals(account.getMustChangePassword()));
        List<RoleEntity> roleEntities = roleRepository.getListRoleByAccountId(account.getId());
        dto.setRoles(roleEntities.stream()
                .map(r -> r.getName() != null ? r.getName().name() : null)
                .filter(Objects::nonNull)
                .toList());
        dto.setPermissions(permissionRepository.getPermissionNamesByAccountId(account.getId()));
    }

    private void updateRevokedRefreshToken(AccountEntity account) {
        final var activeTokens = tokenRepository.findAllActiveByAccountId(account.getId());
        if (!activeTokens.isEmpty()) {
            activeTokens.forEach(tokenEntity -> tokenEntity.setRevoked(true));
            tokenRepository.saveAll(activeTokens);
        }
    }

    private void createRefreshTokenCookie(final HttpServletResponse servletResponse, final Jwt jwt) {
        final var refreshTokenCookie = new Cookie("refresh_token", jwt.getTokenValue());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(rsaKeyProperties.cookieSecure());
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (rsaKeyProperties.refreshExpiredDays() * 24 * 60 * 60));
        servletResponse.addCookie(refreshTokenCookie);
    }

    private void saveRefreshToken(AccountEntity account, Jwt jwt) {
        final var refreshTokenEntity = new TokenEntity();
        refreshTokenEntity.setToken(jwt.getTokenValue());
        refreshTokenEntity.setType(TokenType.REFRESH);
        refreshTokenEntity.setExpiryDate(jwt.getExpiresAt());
        refreshTokenEntity.setRevoked(false);
        refreshTokenEntity.setAccountId(account.getId());
        tokenRepository.save(refreshTokenEntity);
    }

    private void saveAccountRoleMap(Long accountId, RoleEntity role) {
        AccountRoleMapEntity accountRoleMapEntity = AccountRoleMapEntity.builder()
                .accountId(accountId)
                .roleId(role.getId())
                .build();
        accountRoleMapRepository.save(accountRoleMapEntity);
    }

    private void saveResetToken(AccountEntity account, Jwt jwt) {
        final var resetToken = new TokenEntity();
        resetToken.setToken(jwt.getTokenValue());
        resetToken.setType(TokenType.RESET_PASSWORD);
        resetToken.setExpiryDate(jwt.getExpiresAt());
        resetToken.setRevoked(false);
        resetToken.setAccountId(account.getId());
        tokenRepository.save(resetToken);
    }

    private String extractClientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return AuditLogService.extractIp(servletAttrs.getRequest());
        }
        return null;
    }
}
