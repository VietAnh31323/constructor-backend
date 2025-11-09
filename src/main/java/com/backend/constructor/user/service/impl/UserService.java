package com.backend.constructor.user.service.impl;

import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.CloudinaryService;
import com.backend.constructor.user.controller.UserApi;
import com.backend.constructor.user.dto.UserInfoDto;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserApi {
    private final AccountRepository accountRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public UserInfoDto getUserInfo(Long userId) {
        AccountEntity accountEntity = accountRepository.findById(userId).orElseThrow(
                () -> new BusinessException("404", "User with id " + userId + " was not found"));
        return UserInfoDto.builder()
                .userId(userId)
                .username(accountEntity.getUsername())
                .build();
    }

    @Override
    public Long updateUserInfo(UserInfoDto userInfoDto) {
        Long userId = userInfoDto.getUserId();
        AccountEntity accountEntity = accountRepository.findById(userId).orElseThrow(
                () -> new BusinessException("404", "User with id " + userId + " was not found"));
        accountRepository.save(accountEntity);
        log.info("User with id {} updated successfully", userId);
        return userId;
    }
}
