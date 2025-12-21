package com.backend.constructor.core.service;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.port.mapper.StaffMapper;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.backend.constructor.core.service.HelperService.joinName;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService implements AccountApi {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;
    private final AccountStaffMapRepository accountStaffMapRepository;

    @Override
    public StaffDto getAccountInfo() {
        StaffEntity staffEntity = staffRepository.getStaffByUsername(HelperService.getUsernameLogin());
        return staffMapper.toDto(staffEntity);
    }

    @Override
    @Transactional
    public IdResponse updateAccountInfo(StaffDto input) {
        StaffEntity staffEntity = staffRepository.getStaffByUsername(HelperService.getUsernameLogin());
        staffMapper.update(input, staffEntity);
        staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
        staffRepository.save(staffEntity);
        return IdResponse.builder().id(staffEntity.getId()).build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {
        Optional<AccountEntity> accountOptional = accountRepository.findByUsername(HelperService.getUsernameLogin());
        if (accountOptional.isEmpty()) {
            throw BusinessException.exception("CST000");
        }
        AccountEntity accountEntity = accountOptional.get();
        if (!passwordEncoder.matches(changePasswordDto.oldPassword(), accountEntity.getPassword())) {
            throw BusinessException.exception("ERROR_0003");
        }
        accountEntity.setPassword(passwordEncoder.encode(changePasswordDto.newPassword()));
        accountRepository.save(accountEntity);
    }

    @Override
    @Transactional
    public void assignStaffForAccount(AssignStaffDto input) {
        StaffEntity staffEntity = staffRepository.getStaffById(input.getStaff().getId());
        AccountEntity accountEntity = accountRepository.getAccountById(input.getAccountId());
        AccountStaffMapEntity accountStaffMapEntity = AccountStaffMapEntity.builder()
                .accountId(accountEntity.getId())
                .staffId(staffEntity.getId())
                .build();
        accountStaffMapRepository.save(accountStaffMapEntity);
    }
}
