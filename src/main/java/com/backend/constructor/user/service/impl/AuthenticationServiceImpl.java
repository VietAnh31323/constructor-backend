package com.backend.constructor.user.service.impl;

import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.common.enums.TokenType;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.MailService;
import com.backend.constructor.config.languages.Translator;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.AuthScheme;
import com.backend.constructor.core.port.mapper.AccountMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.core.service.HelperService;
import com.backend.constructor.user.dto.request.*;
import com.backend.constructor.user.dto.response.AccountDto;
import com.backend.constructor.user.security.jwt.TokenProvider;
import com.backend.constructor.user.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
    //Other
    PasswordEncoder passwordEncoder;
    AuthenticationManagerBuilder authenticationManagerBuilder;
    TokenProvider tokenProvider;
    Translator translator;

    //Repository
    AccountRepository accountRepository;
    RoleRepository roleRepository;
    TokenRepository tokenRepository;

    //Mapper
    AccountMapper accountMapper;
    private final AccountRoleMapRepository accountRoleMapRepository;
    private final MailService mailService;
    private final PasswordResetRepository passwordResetRepository;

    @Override
    @Transactional
    public IdResponse signUp(SignUpRequest request) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new BusinessException(String.valueOf(HttpStatus.BAD_REQUEST.value()), translator.toLocale("CST014", request.username()));
        }
        final var entity = accountMapper.toEntity(request);
        RoleEntity role = roleRepository.getByName(ERole.STAFF).orElse(RoleEntity.builder().name(ERole.STAFF).description("nhân viên").build());
        entity.setPassword(passwordEncoder.encode(request.password()));
        entity.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(entity);
        saveAccountRoleMap(entity.getId(), role);
        mailService.sendPasswordEmail(entity.getUsername(), request.password(), role.getName().getValue());
        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public AccountDto signIn(SignInRequest request, HttpServletResponse response) {
        final var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                request.username().trim(),
                request.password().trim()
        );

        final var authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final var jwtAccessToken = tokenProvider.createToken(authentication);
        final var jwtRefreshToken = tokenProvider.createRefreshToken(authentication);

        final var username = tokenProvider.getUserName(jwtAccessToken);
        final var account = accountRepository.findByUsernameAndRole(username, request.eRole());

        if (account.getStatus().equals(AccountStatus.INACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, translator.toLocale("CST001"));
        }
        return getAccountDto(response, account, jwtAccessToken, jwtRefreshToken);
    }

    @Override
    public AccountDto accessTokenByRefreshToken(HttpServletRequest req) {
        final var refreshToken = tokenProvider.resolveToken(req);

        if (ObjectUtils.isEmpty(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please verify your token");
        }
        //Find refreshToken from database and should not be revoked : Same thing can be done through filter.
        var refreshTokenEntity = tokenRepository.findByRefreshToken(refreshToken)
                .filter(tokens -> !tokens.getRevoked())
                .orElseThrow(() -> BusinessException.exception("Refresh token revoked"));

        final var account = accountRepository.getAccountById(refreshTokenEntity.getAccountId());

        updateRevokedRefreshToken(account);

        //Now create the Authentication object
        final var authentication = tokenProvider.getAuthentication(account);

        //Use the authentication object to generate new jwt as the Authentication object that we will have may not contain correct role.
        final var jwtAccessToken = tokenProvider.createToken(authentication);

        return accountMapper.toDto(jwtAccessToken);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordDto input) {
        Optional<AccountEntity> account = accountRepository.findOptionalByUsernameAndRole(input.username(), input.eRole());
        if (account.isEmpty()) {
            return;
        }
        AccountEntity accountEntity = account.get();
        String generateOtp = HelperService.generateOtp();
        final var now = Instant.now();
        Instant expiresAt = now.plus(5, ChronoUnit.MINUTES);
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
        var refreshTokenEntity = tokenRepository.findByResetToken(resetToken);
        if (!tokenProvider.validateToken(refreshTokenEntity.getToken())) {
            throw BusinessException.exception("CST009");
        }
        AccountEntity accountEntity = accountRepository.getAccountById(refreshTokenEntity.getAccountId());
        accountEntity.setPassword(passwordEncoder.encode(input.newPassword()));
        accountRepository.save(accountEntity);
    }

    private AccountDto getAccountDto(HttpServletResponse response, AccountEntity account, Jwt jwtAccessToken, Jwt jwtRefreshToken) {
        updateRevokedRefreshToken(account);

        saveRefreshToken(account, jwtRefreshToken);

        createRefreshTokenCookie(response, jwtRefreshToken);

        AccountDto dto = accountMapper.toDto(jwtAccessToken, jwtRefreshToken);
        dto.setUserId(account.getId());
        return dto;
    }

    private void updateRevokedRefreshToken(AccountEntity account) {
        final var refreshTokenEntities = tokenRepository.findByAccountAndRevoked(account.getId(), false);
        refreshTokenEntities.forEach(tokenEntity -> tokenEntity.setRevoked(true));

        tokenRepository.saveAll(refreshTokenEntities);
    }

    private void createRefreshTokenCookie(final HttpServletResponse servletResponse, final Jwt jwt) {
        final var refreshTokenCookie = new Cookie("refresh_token", jwt.getTokenValue());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(15 * 24 * 60 * 60); // 15 day to seconds
        servletResponse.addCookie(refreshTokenCookie);
    }

    private void saveRefreshToken(AccountEntity account, Jwt jwt) {
        final var refreshTokenEntity = new TokenEntity();
        refreshTokenEntity.setToken(jwt.getTokenValue());
        refreshTokenEntity.setType(TokenType.REFRESH);
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
        resetToken.setRevoked(false);
        resetToken.setAccountId(account.getId());
        tokenRepository.save(resetToken);
    }
}
