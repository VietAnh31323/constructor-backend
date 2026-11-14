package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.account.*;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface AccountApi {
    StaffDto getAccountInfo();

    IdResponse updateAccountInfo(StaffDto userInfoDto);

    void changePassword(ChangePasswordDto changePasswordDto);

    void assignStaffForAccount(AssignStaffDto input);

    Page<AccountStaffDto> getPageAccount(String search,
                                         AccountStatus accountStatus,
                                         Pageable pageable);

    AccountStaffDto getDetailStaffByAccountId(Long accountId);

    IdResponse updateRoleOfAccount(AccountStaffDto accountStaffDto);

    IdResponse lockAccount(Long accountId);

    IdResponse unlockAccount(Long accountId);

    AdminResetPasswordResponse adminResetPassword(Long accountId, AdminResetPasswordDto input);

    IdResponse updateForceChangePassword(Long accountId, ForceChangePasswordDto input);

    List<RoleDto> getAllRoles();

    List<AccountSimpleDto> getUnassignedAccounts();

    Set<String> getCurrentUserPermissions();

    List<PermissionDto> getAllPermissions();
}
