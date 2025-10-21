package com.backend.constructor.user.service.impl;

import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.CloudinaryService;
import com.backend.constructor.user.controller.UserApi;
import com.backend.constructor.user.dto.UserInfoDto;
import com.backend.constructor.user.entity.AccountEntity;
import com.backend.constructor.user.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
                .displayName(accountEntity.getDisplayName())
                .avatar(accountEntity.getAvatar() != null ? accountEntity.getAvatar() : "https://res.cloudinary.com/dt8idd99e/image/upload/v1748926635/images_kywpvl.png")
                .build();
    }

    @Override
    public Long updateUserInfo(UserInfoDto userInfoDto) {
        Long userId = userInfoDto.getUserId();
        AccountEntity accountEntity = accountRepository.findById(userId).orElseThrow(
                () -> new BusinessException("404", "User with id " + userId + " was not found"));
        accountEntity.setDisplayName(userInfoDto.getDisplayName());
        try {
            if (userInfoDto.getAvatarFile() != null)
                accountEntity.setAvatar(cloudinaryService.uploadImage(userInfoDto.getAvatarFile()));
        } catch (IOException e) {
            throw new BusinessException("400", e.getMessage());
        }
        accountRepository.save(accountEntity);
        log.info("User with id {} updated successfully", userId);
        return userId;
    }
}
