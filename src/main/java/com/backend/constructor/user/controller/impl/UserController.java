package com.backend.constructor.user.controller.impl;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.user.dto.UserInfoDto;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class UserController {
    private final AccountApi userService;

    @GetMapping
    public UserInfoDto getAccountInfo(@RequestParam Long userId) {
        return userService.getAccountInfo(userId);
    }

    @PutMapping
    public IdResponse updateAccountInfo(@RequestBody @Valid UserInfoDto userInfoDto) {
        return userService.updateAccountInfo(userInfoDto);
    }

    @PutMapping("/change-password")
    public void changePassword(@RequestBody @Valid ChangePasswordDto input) {
        userService.changePassword(input);
    }
}
