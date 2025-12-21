package com.backend.constructor.user.service.impl;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.service.HelperService;
import com.backend.constructor.user.dto.UserInfoDto;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService implements AccountApi {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserInfoDto getAccountInfo(Long userId) {
        AccountEntity accountEntity = accountRepository.getAccountById(userId);
        return UserInfoDto.builder()
                .userId(userId)
                .username(accountEntity.getUsername())
                .build();
    }

    @Override
    @Transactional
    public IdResponse updateAccountInfo(UserInfoDto userInfoDto) {
        Long userId = userInfoDto.getUserId();
        AccountEntity accountEntity = accountRepository.getAccountById(userId);
        accountRepository.save(accountEntity);
        log.info("User with id {} updated successfully", userId);
        return IdResponse.builder().id(userId).build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {
        Optional<AccountEntity> accountOptional = accountRepository.findByUsername(HelperService.getUsernameLogin());
        if (accountOptional.isEmpty()) {
            throw BusinessException.exception("CST000");
        }
        AccountEntity accountEntity = accountOptional.get();
        if (!passwordEncoder.matches(changePasswordDto.oldPassword(), accountEntity.getPassword())) {
            throw BusinessException.exception("ERROR_0003");
        }
        accountEntity.setPassword(passwordEncoder.encode(changePasswordDto.newPassword()));
        accountRepository.save(accountEntity);
    }
}
