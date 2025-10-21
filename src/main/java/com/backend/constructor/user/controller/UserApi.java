package com.backend.constructor.user.controller;


import com.backend.constructor.user.dto.UserInfoDto;

public interface UserApi {
    UserInfoDto getUserInfo(Long userId);

    Long updateUserInfo(UserInfoDto userInfoDto);
}
