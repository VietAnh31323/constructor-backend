package com.backend.constructor.user.controller.impl;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.Message;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController implements AccountApi {
    private final AccountApi accountService;

    @Override
    @GetMapping
    public StaffDto getAccountInfo() {
        return accountService.getAccountInfo();
    }

    @Override
    @PutMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN','SCOPE_STAFF')")
    public IdResponse updateAccountInfo(@RequestBody @Valid StaffDto userInfoDto) {
        return accountService.updateAccountInfo(userInfoDto);
    }

    @Override
    @PutMapping("/change-password")
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN','SCOPE_STAFF')")
    public void changePassword(@RequestBody @Valid ChangePasswordDto input) {
        accountService.changePassword(input);
    }

    @Override
    @PutMapping("/assign")
    @Message("SUCCESS_005")
    public void assignStaffForAccount(@RequestBody @Valid AssignStaffDto input) {
        accountService.assignStaffForAccount(input);
    }
}
