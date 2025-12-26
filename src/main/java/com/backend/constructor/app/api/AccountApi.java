package com.backend.constructor.app.api;


import com.backend.constructor.app.dto.account.AccountStaffDto;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
