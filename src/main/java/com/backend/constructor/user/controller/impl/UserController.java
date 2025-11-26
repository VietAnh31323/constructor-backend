package com.backend.constructor.user.controller.impl;

import com.backend.constructor.user.dto.UserInfoDto;
import com.backend.constructor.user.service.impl.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public UserInfoDto getUserInfo(@RequestParam Long userId) {
        return userService.getUserInfo(userId);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long updateUserInfo(@Valid UserInfoDto userInfoDto) {
        return userService.updateUserInfo(userInfoDto);
    }
}
