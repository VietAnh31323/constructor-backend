package com.backend.constructor.core.service;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.app.dto.account.AccountStaffDto;
import com.backend.constructor.app.dto.account.RoleDto;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.port.mapper.AccountMapper;
import com.backend.constructor.core.port.mapper.StaffMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.backend.constructor.core.service.HelperService.*;

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
    private final AccountMapper accountMapper;
    private final RoleRepository roleRepository;
    private final AccountRoleMapRepository accountRoleMapRepository;

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
        if (!Objects.equals(changePasswordDto.newPassword(), changePasswordDto.confirmPassword())) {
            throw BusinessException.exception("CST017");
        }
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

    @Override
    public Page<AccountStaffDto> getPageAccount(String search, AccountStatus accountStatus, Pageable pageable) {
        Page<AccountEntity> accountEntities = accountRepository.getPageAccount(search, accountStatus, pageable);
        Set<Long> accountIds = new HashSet<>();
        for (AccountEntity accountEntity : accountEntities) {
            addIfNotNull(accountIds, accountEntity.getId());
        }
        Map<Long, List<RoleDto>> map = roleRepository.getMapRoleDtoByAccountIds(accountIds);
        List<StaffEntity> staffEntities = staffRepository.getListStaffByAccountIds(accountIds);
        Map<Long, StaffDto> staffMap = getMapStaffByAccountId(staffEntities);
        return accountEntities.map(accountEntity -> buildAccountOutput(accountEntity, map, staffMap));
    }

    @Override
    public AccountStaffDto getDetailStaffByAccountId(Long accountId) {
        AccountEntity accountEntity = accountRepository.getAccountById(accountId);
        Map<Long, List<RoleDto>> roleMap = roleRepository.getMapRoleDtoByAccountIds(Set.of(accountId));
        StaffEntity staffEntity = staffRepository.getStaffByAccountId(accountId);
        StaffDto staffDto = staffMapper.toDto(staffEntity);
        return AccountStaffDto.builder()
                .id(accountEntity.getId())
                .username(accountEntity.getUsername())
                .staff(staffDto)
                .roles(getData(roleMap, accountEntity.getId()))
                .build();
    }

    @Override
    @Transactional
    public IdResponse updateRoleOfAccount(AccountStaffDto accountStaffDto) {
        AccountEntity accountEntity = accountRepository.getAccountById(accountStaffDto.getId());
        List<AccountRoleMapEntity> accountRoleMapEntities = accountRoleMapRepository.getListByAccountId(accountEntity.getId());
        accountRoleMapRepository.deleteAllInBatch(accountRoleMapEntities);
        List<AccountRoleMapEntity> roleMapEntities = new ArrayList<>();
        for (RoleDto role : accountStaffDto.getRoles()) {
            AccountRoleMapEntity accountRoleMapEntity = AccountRoleMapEntity.builder()
                    .accountId(accountEntity.getId())
                    .roleId(role.getId())
                    .build();
            roleMapEntities.add(accountRoleMapEntity);
        }
        accountRoleMapRepository.saveAll(roleMapEntities);
        return IdResponse.builder().id(accountEntity.getId()).build();
    }

    private Map<Long, StaffDto> getMapStaffByAccountId(List<StaffEntity> staffEntities) {
        Map<Long, StaffDto> map = new HashMap<>();
        for (StaffEntity staffEntity : staffEntities) {
            map.put(staffEntity.getAccountId(), staffMapper.toDto(staffEntity));
        }
        return map;
    }

    private AccountStaffDto buildAccountOutput(AccountEntity accountEntity,
                                               Map<Long, List<RoleDto>> map,
                                               Map<Long, StaffDto> staffMap) {
        AccountStaffDto accountStaffDto = accountMapper.toOutput(accountEntity);
        accountStaffDto.setRoles(getData(map, accountEntity.getId()));
        accountStaffDto.setStaff(getData(staffMap, accountEntity.getId()));
        return accountStaffDto;
    }
}
