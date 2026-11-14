package com.backend.constructor.user.controller.impl;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.app.dto.account.*;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.Message;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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
    public IdResponse updateAccountInfo(@RequestBody @Valid StaffDto userInfoDto) {
        return accountService.updateAccountInfo(userInfoDto);
    }

    @Override
    @PutMapping("/change-password")
    public void changePassword(@RequestBody @Valid ChangePasswordDto input) {
        accountService.changePassword(input);
    }

    @Override
    @PutMapping("/assign")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    @Message("SUCCESS_005")
    public void assignStaffForAccount(@RequestBody @Valid AssignStaffDto input) {
        accountService.assignStaffForAccount(input);
    }

    @Override
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'SCOPE_ADMIN')")
    @HandsomePaging
    public Page<AccountStaffDto> getPageAccount(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) AccountStatus accountStatus,
                                                @ParameterObject Pageable pageable) {
        return accountService.getPageAccount(search, accountStatus, pageable);
    }

    @Override
    @GetMapping("/staff")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'SCOPE_ADMIN')")
    public AccountStaffDto getDetailStaffByAccountId(@RequestParam Long accountId) {
        return accountService.getDetailStaffByAccountId(accountId);
    }

    @Override
    @PutMapping("/role")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    public IdResponse updateRoleOfAccount(@RequestBody @Valid AccountStaffDto accountStaffDto) {
        return accountService.updateRoleOfAccount(accountStaffDto);
    }

    @Override
    @PutMapping("/{accountId}/lock")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    public IdResponse lockAccount(@PathVariable Long accountId) {
        return accountService.lockAccount(accountId);
    }

    @Override
    @PutMapping("/{accountId}/unlock")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    public IdResponse unlockAccount(@PathVariable Long accountId) {
        return accountService.unlockAccount(accountId);
    }

    @Override
    @PostMapping("/{accountId}/reset-password")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    public AdminResetPasswordResponse adminResetPassword(@PathVariable Long accountId,
                                                         @RequestBody(required = false) AdminResetPasswordDto input) {
        return accountService.adminResetPassword(accountId, input);
    }

    @Override
    @PutMapping("/{accountId}/force-change-password")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    public IdResponse updateForceChangePassword(@PathVariable Long accountId,
                                                @RequestBody @Valid ForceChangePasswordDto input) {
        return accountService.updateForceChangePassword(accountId, input);
    }

    @Override
    @GetMapping("/roles")
    public List<RoleDto> getAllRoles() {
        return accountService.getAllRoles();
    }

    @Override
    @GetMapping("/permissions")
    public Set<String> getCurrentUserPermissions() {
        return accountService.getCurrentUserPermissions();
    }

    @Override
    @GetMapping("/all-permissions")
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    public List<PermissionDto> getAllPermissions() {
        return accountService.getAllPermissions();
    }
}
