package com.backend.constructor.core.service;

import com.backend.constructor.app.api.AccountApi;
import com.backend.constructor.app.dto.account.*;
import com.backend.constructor.app.dto.staff.AssignStaffDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.service.MailService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.mapper.AccountMapper;
import com.backend.constructor.core.port.mapper.StaffMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.user.dto.request.ChangePasswordDto;
import com.backend.constructor.user.service.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final StaffCertificateRepository staffCertificateRepository;
    private final StaffMapper staffMapper;
    private final com.backend.constructor.core.port.mapper.StaffCertificateMapper staffCertificateMapper;
    private final AccountStaffMapRepository accountStaffMapRepository;
    private final AccountMapper accountMapper;
    private final RoleRepository roleRepository;
    private final AccountRoleMapRepository accountRoleMapRepository;
    private final PermissionRepository permissionRepository;
    private final GenerateCodeService generateCodeService;
    private final TokenRevocationService tokenRevocationService;
    private final MailService mailService;
    private final AuditLogService auditLogService;

    @Override
    public StaffDto getAccountInfo() {
        String username = HelperService.getUsernameLogin();
        Optional<StaffEntity> staffOpt = staffRepository.findOptionalStaffByUsername(username);
        if (staffOpt.isPresent()) {
            StaffEntity staff = staffOpt.get();
            StaffDto dto = staffMapper.toDto(staff);
            java.time.LocalDate today = java.time.LocalDate.now();
            dto.setCertificates(
                    staffCertificateRepository.getListByStaffId(staff.getId()).stream()
                            .map(cert -> {
                                var certDto = staffCertificateMapper.toDto(cert);
                                certDto.computeValidity(today);
                                return certDto;
                            })
                            .toList()
            );
            return dto;
        }
        AccountEntity account = accountRepository.findAnyByUsername(username)
                .orElseThrow(() -> BusinessException.exception("CST000"));
        StaffDto fallback = new StaffDto();
        fallback.setEmail(account.getUsername());
        fallback.setFirstName(account.getFirstName());
        fallback.setLastName(account.getLastName());
        fallback.setName(joinName(account.getFirstName(), account.getLastName()));
        return fallback;
    }

    @Override
    @Transactional
    public IdResponse updateAccountInfo(StaffDto input) {
        String username = HelperService.getUsernameLogin();
        Optional<StaffEntity> staffOpt = staffRepository.findOptionalStaffByUsername(username);
        if (staffOpt.isPresent()) {
            StaffEntity staffEntity = staffOpt.get();
            if (input.getCode() == null) input.setCode(staffEntity.getCode());
            if (input.getEmail() == null) input.setEmail(staffEntity.getEmail());
            if (input.getPosition() == null) input.setPosition(staffEntity.getPosition());
            if (input.getDepartment() == null) input.setDepartment(staffEntity.getDepartment());
            if (input.getJoinDate() == null) input.setJoinDate(staffEntity.getJoinDate());
            if (input.getStatus() == null) input.setStatus(staffEntity.getStatus());
            if (input.getIdCardNumber() == null) input.setIdCardNumber(staffEntity.getIdCardNumber());
            if (input.getIdCardIssueDate() == null) input.setIdCardIssueDate(staffEntity.getIdCardIssueDate());
            if (input.getIdCardIssuePlace() == null) input.setIdCardIssuePlace(staffEntity.getIdCardIssuePlace());
            generateCodeService.generateCode(input, Constants.NS, StaffEntity.class);
            staffMapper.update(input, staffEntity);
            staffEntity.setName(joinName(input.getFirstName(), input.getLastName()));
            staffRepository.save(staffEntity);
            return IdResponse.builder().id(staffEntity.getId()).build();
        }
        AccountEntity account = accountRepository.findAnyByUsername(username)
                .orElseThrow(() -> BusinessException.exception("CST000"));
        account.setFirstName(input.getFirstName());
        account.setLastName(input.getLastName());
        accountRepository.save(account);
        return IdResponse.builder().id(account.getId()).build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordDto changePasswordDto) {
        if (!Objects.equals(changePasswordDto.newPassword(), changePasswordDto.confirmPassword())) {
            throw BusinessException.exception("CST017");
        }
        AccountEntity accountEntity = accountRepository.findAnyByUsername(HelperService.getUsernameLogin())
                .orElseThrow(() -> BusinessException.exception("CST000"));

        if (!passwordEncoder.matches(changePasswordDto.oldPassword(), accountEntity.getPassword())) {
            throw BusinessException.exception("ERROR_0004");
        }
        if (passwordEncoder.matches(changePasswordDto.newPassword(), accountEntity.getPassword())) {
            throw BusinessException.exception("CST023");
        }

        Boolean prevMustChange = accountEntity.getMustChangePassword();
        accountEntity.setPassword(passwordEncoder.encode(changePasswordDto.newPassword()));
        accountEntity.setMustChangePassword(false);
        accountEntity.setFailedLoginAttempts(0);
        accountEntity.setLockedUntil(null);
        accountRepository.save(accountEntity);

        tokenRevocationService.revokeAllTokens(accountEntity);

        auditLogService.logExplicit(
                "CHANGE_PASSWORD",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(accountEntity.getId()),
                Map.of("mustChangePassword", Boolean.TRUE.equals(prevMustChange)),
                Map.of("mustChangePassword", false, "tokensRevoked", true),
                "Người dùng đổi mật khẩu tài khoản " + accountEntity.getUsername(),
                accountEntity.getUsername()
        );
    }

    @Override
    @Transactional
    public void assignStaffForAccount(AssignStaffDto input) {
        if (input == null || input.getStaff() == null || input.getStaff().getId() == null || input.getAccountId() == null) {
            throw BusinessException.exception("ERROR_0001");
        }
        StaffEntity staffEntity = staffRepository.getStaffById(input.getStaff().getId());
        AccountEntity accountEntity = accountRepository.getAccountById(input.getAccountId());

        Optional<AccountStaffMapEntity> existingByAccount = accountStaffMapRepository.findOptionalByAccountId(accountEntity.getId());
        if (existingByAccount.isPresent()) {
            if (Objects.equals(existingByAccount.get().getStaffId(), staffEntity.getId())) {
                return;
            }
            throw BusinessException.exception("CST030");
        }

        List<AccountStaffMapEntity> existingByStaff = accountStaffMapRepository.getListByStaffId(staffEntity.getId());
        if (!existingByStaff.isEmpty()) {
            throw BusinessException.exception("CST031");
        }

        AccountStaffMapEntity accountStaffMapEntity = AccountStaffMapEntity.builder()
                .accountId(accountEntity.getId())
                .staffId(staffEntity.getId())
                .build();
        accountStaffMapRepository.save(accountStaffMapEntity);

        auditLogService.logExplicit(
                "ASSIGN_ACCOUNT_TO_STAFF",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(accountEntity.getId()),
                null,
                Map.of("staffId", staffEntity.getId(), "staffCode", String.valueOf(staffEntity.getCode())),
                "Gán tài khoản " + accountEntity.getUsername() + " cho nhân sự " + staffEntity.getName(),
                null
        );
    }

    @Override
    public Page<AccountStaffDto> getPageAccount(String search, AccountStatus accountStatus, Pageable pageable) {
        Page<AccountEntity> accountEntities = accountRepository.getPageAccount(search, accountStatus, pageable);
        Set<Long> accountIds = new HashSet<>();
        for (AccountEntity accountEntity : accountEntities) {
            addIfNotNull(accountIds, accountEntity.getId());
        }
        Map<Long, List<RoleDto>> map = accountIds.isEmpty()
                ? Collections.emptyMap()
                : roleRepository.getMapRoleDtoByAccountIds(accountIds);
        List<StaffEntity> staffEntities = staffRepository.getListStaffByAccountIds(accountIds);
        Map<Long, StaffDto> staffMap = getMapStaffByAccountId(staffEntities);
        return accountEntities.map(accountEntity -> buildAccountOutput(accountEntity, map, staffMap));
    }

    @Override
    public AccountStaffDto getDetailStaffByAccountId(Long accountId) {
        AccountEntity accountEntity = accountRepository.getAccountById(accountId);
        Map<Long, List<RoleDto>> roleMap = roleRepository.getMapRoleDtoByAccountIds(Set.of(accountId));
        StaffDto staffDto = staffRepository.findOptionalStaffByAccountId(accountId)
                .map(staffMapper::toDto)
                .orElse(null);
        return AccountStaffDto.builder()
                .id(accountEntity.getId())
                .username(accountEntity.getUsername())
                .firstName(accountEntity.getFirstName())
                .lastName(accountEntity.getLastName())
                .status(accountEntity.getStatus())
                .mustChangePassword(Boolean.TRUE.equals(accountEntity.getMustChangePassword()))
                .failedLoginAttempts(accountEntity.getFailedLoginAttempts())
                .lockedUntil(accountEntity.getLockedUntil())
                .createdAt(accountEntity.getCreatedAt())
                .staff(staffDto)
                .roles(getData(roleMap, accountEntity.getId()))
                .build();
    }

    @Override
    @Transactional
    public IdResponse updateRoleOfAccount(AccountStaffDto accountStaffDto) {
        AccountEntity accountEntity = accountRepository.getAccountById(accountStaffDto.getId());
        List<RoleEntity> oldRoles = roleRepository.getListRoleByAccountId(accountEntity.getId());
        List<String> oldRoleNames = oldRoles.stream()
                .map(r -> r.getName() != null ? r.getName().name() : "")
                .toList();

        List<AccountRoleMapEntity> accountRoleMapEntities = accountRoleMapRepository.getListByAccountId(accountEntity.getId());
        accountRoleMapRepository.deleteAllInBatch(accountRoleMapEntities);

        List<AccountRoleMapEntity> roleMapEntities = new ArrayList<>();
        if (accountStaffDto.getRoles() != null) {
            for (RoleDto role : accountStaffDto.getRoles()) {
                Long roleId = role.getId();
                if (roleId == null && role.getName() != null) {
                    roleId = roleRepository.getByName(role.getName()).map(RoleEntity::getId).orElse(null);
                }
                if (roleId != null) {
                    roleMapEntities.add(AccountRoleMapEntity.builder()
                            .accountId(accountEntity.getId())
                            .roleId(roleId)
                            .build());
                }
            }
        }
        if (!roleMapEntities.isEmpty()) {
            accountRoleMapRepository.saveAll(roleMapEntities);
        }

        if (accountStaffDto.getMustChangePassword() != null) {
            accountEntity.setMustChangePassword(accountStaffDto.getMustChangePassword());
            accountRepository.save(accountEntity);
        }

        List<RoleEntity> newRoles = roleRepository.getListRoleByAccountId(accountEntity.getId());
        List<String> newRoleNames = newRoles.stream()
                .map(r -> r.getName() != null ? r.getName().name() : "")
                .toList();

        auditLogService.logExplicit(
                "UPDATE_ROLE",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(accountEntity.getId()),
                Map.of("roles", oldRoleNames),
                Map.of("roles", newRoleNames),
                "Cập nhật phân quyền cho tài khoản " + accountEntity.getUsername(),
                null
        );

        return IdResponse.builder().id(accountEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse lockAccount(Long accountId) {
        AccountEntity account = accountRepository.getAccountById(accountId);
        String currentUsername = HelperService.getUsernameLogin();
        if (Objects.equals(account.getUsername(), currentUsername)) {
            throw BusinessException.exception("CST022");
        }
        AccountStatus oldStatus = account.getStatus();
        account.setStatus(AccountStatus.LOCKED);
        accountRepository.save(account);

        tokenRevocationService.revokeAllTokens(account);

        auditLogService.logExplicit(
                "LOCK_ACCOUNT",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(account.getId()),
                Map.of("status", oldStatus != null ? oldStatus.name() : ""),
                Map.of("status", AccountStatus.LOCKED.name(), "tokensRevoked", true),
                "Khóa tài khoản " + account.getUsername(),
                null
        );

        return IdResponse.builder().id(account.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse unlockAccount(Long accountId) {
        AccountEntity account = accountRepository.getAccountById(accountId);
        AccountStatus oldStatus = account.getStatus();
        account.setStatus(AccountStatus.ACTIVE);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        accountRepository.save(account);

        auditLogService.logExplicit(
                "UNLOCK_ACCOUNT",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(account.getId()),
                Map.of("status", oldStatus != null ? oldStatus.name() : ""),
                Map.of("status", AccountStatus.ACTIVE.name(), "failedLoginAttempts", 0),
                "Mở khóa tài khoản " + account.getUsername(),
                null
        );

        return IdResponse.builder().id(account.getId()).build();
    }

    @Override
    @Transactional
    public AdminResetPasswordResponse adminResetPassword(Long accountId, AdminResetPasswordDto input) {
        AccountEntity account = accountRepository.getAccountById(accountId);
        String rawPassword = (input != null && StringUtils.hasText(input.getNewPassword()))
                ? input.getNewPassword().trim()
                : PasswordGenerator.generateDefaultPassword(8);

        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setMustChangePassword(true);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        accountRepository.save(account);

        staffRepository.findOptionalStaffByAccountId(accountId).ifPresent(staff -> {
            staff.setGenPassword(rawPassword);
            staffRepository.save(staff);
        });

        tokenRevocationService.revokeAllTokens(account);

        List<RoleEntity> roles = roleRepository.getListRoleByAccountId(accountId);
        String roleLabel = roles.isEmpty() || roles.getFirst().getName() == null
                ? "Nhân viên"
                : roles.getFirst().getName().getValue();
        mailService.sendPasswordEmail(account.getUsername(), rawPassword, roleLabel);

        auditLogService.logExplicit(
                "ADMIN_RESET_PASSWORD",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(account.getId()),
                null,
                Map.of("mustChangePassword", true, "tokensRevoked", true),
                "Quản trị viên đặt lại mật khẩu tạm thời cho tài khoản " + account.getUsername(),
                null
        );

        return AdminResetPasswordResponse.builder()
                .accountId(account.getId())
                .username(account.getUsername())
                .temporaryPassword(rawPassword)
                .mustChangePassword(true)
                .build();
    }

    @Override
    @Transactional
    public IdResponse updateForceChangePassword(Long accountId, ForceChangePasswordDto input) {
        AccountEntity account = accountRepository.getAccountById(accountId);
        Boolean prev = account.getMustChangePassword();
        account.setMustChangePassword(input.getMustChangePassword());
        accountRepository.save(account);

        auditLogService.logExplicit(
                "UPDATE_FORCE_PASSWORD_CHANGE",
                "ACCOUNT",
                "AccountEntity",
                String.valueOf(account.getId()),
                Map.of("mustChangePassword", Boolean.TRUE.equals(prev)),
                Map.of("mustChangePassword", Boolean.TRUE.equals(input.getMustChangePassword())),
                "Cập nhật yêu cầu đổi mật khẩu lần đầu cho tài khoản " + account.getUsername(),
                null
        );

        return IdResponse.builder().id(account.getId()).build();
    }

    @Override
    public List<RoleDto> getAllRoles() {
        return roleRepository.getAllRoles().stream()
                .map(r -> RoleDto.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .description(r.getDescription() != null ? r.getDescription() : (r.getName() != null ? r.getName().getValue() : null))
                        .build())
                .toList();
    }

    @Override
    public List<AccountSimpleDto> getUnassignedAccounts() {
        List<AccountEntity> unassigned = accountRepository.getUnassignedAccounts();
        if (unassigned.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> accountIds = new HashSet<>();
        for (AccountEntity acc : unassigned) {
            addIfNotNull(accountIds, acc.getId());
        }
        Map<Long, List<RoleDto>> roleMap = roleRepository.getMapRoleDtoByAccountIds(accountIds);
        return unassigned.stream()
                .map(acc -> AccountSimpleDto.builder()
                        .id(acc.getId())
                        .username(acc.getUsername())
                        .firstName(acc.getFirstName())
                        .lastName(acc.getLastName())
                        .status(acc.getStatus())
                        .roles(getData(roleMap, acc.getId()))
                        .build())
                .toList();
    }

    @Override
    public Set<String> getCurrentUserPermissions() {
        String username = HelperService.getUsernameLogin();
        AccountEntity account = accountRepository.findAnyByUsername(username)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        return permissionRepository.getPermissionNamesByAccountId(account.getId());
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.getAllPermissions().stream()
                .map(p -> PermissionDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .module(p.getModule())
                        .build())
                .toList();
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
        accountStaffDto.setStatus(accountEntity.getStatus());
        accountStaffDto.setMustChangePassword(Boolean.TRUE.equals(accountEntity.getMustChangePassword()));
        accountStaffDto.setFailedLoginAttempts(accountEntity.getFailedLoginAttempts());
        accountStaffDto.setLockedUntil(accountEntity.getLockedUntil());
        accountStaffDto.setCreatedAt(accountEntity.getCreatedAt());
        accountStaffDto.setRoles(getData(map, accountEntity.getId()));
        accountStaffDto.setStaff(getData(staffMap, accountEntity.getId()));
        return accountStaffDto;
    }
}
