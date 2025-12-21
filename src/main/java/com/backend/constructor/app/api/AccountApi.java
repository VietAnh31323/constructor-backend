package com.backend.constructor.app.api;


import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.user.dto.UserInfoDto;
import com.backend.constructor.user.dto.request.ChangePasswordDto;

public interface AccountApi {
    UserInfoDto getAccountInfo(Long userId);

    IdResponse updateAccountInfo(UserInfoDto userInfoDto);

    void changePassword(ChangePasswordDto changePasswordDto);
}
