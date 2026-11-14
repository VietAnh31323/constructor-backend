package com.backend.constructor.config.init;

import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.EPermission;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import com.backend.constructor.core.domain.entity.PermissionEntity;
import com.backend.constructor.core.domain.entity.RoleEntity;
import com.backend.constructor.core.domain.entity.RolePermissionMapEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.AccountRoleMapRepository;
import com.backend.constructor.core.port.repository.PermissionRepository;
import com.backend.constructor.core.port.repository.RolePermissionMapRepository;
import com.backend.constructor.core.port.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountInit implements CommandLineRunner {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AccountRoleMapRepository accountRoleMapRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionMapRepository rolePermissionMapRepository;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Init roles");
        for (ERole eRole : ERole.values()) {
            if (roleRepository.getByName(eRole).isEmpty()) {
                roleRepository.save(RoleEntity.builder()
                        .name(eRole)
                        .description(eRole.getValue())
                        .build());
            }
        }

        log.info("Init permissions");
        for (EPermission perm : EPermission.values()) {
            if (permissionRepository.getByName(perm.name()).isEmpty()) {
                permissionRepository.save(PermissionEntity.builder()
                        .name(perm.name())
                        .description(perm.getDescription())
                        .module(perm.getModule())
                        .build());
            }
        }

        log.info("Sync admin role permissions");
        RoleEntity roleEntity = roleRepository.getByName(ERole.ADMIN)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name(ERole.ADMIN)
                        .description(ERole.ADMIN.getValue())
                        .build()));

        List<PermissionEntity> allPerms = permissionRepository.getAllPermissions();
        List<RolePermissionMapEntity> existing = rolePermissionMapRepository.getListByRoleId(roleEntity.getId());
        Set<Long> existingPermIds = existing.stream().map(RolePermissionMapEntity::getPermissionId).collect(Collectors.toSet());
        List<RolePermissionMapEntity> toAdd = new ArrayList<>();
        for (PermissionEntity p : allPerms) {
            if (!existingPermIds.contains(p.getId())) {
                toAdd.add(RolePermissionMapEntity.builder()
                        .roleId(roleEntity.getId())
                        .permissionId(p.getId())
                        .build());
            }
        }
        if (!toAdd.isEmpty()) {
            rolePermissionMapRepository.saveAll(toAdd);
        }

        if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPassword)) {
            log.info("Initial admin credentials not configured via environment; skipping admin account creation");
            return;
        }

        if (accountRepository.existsByUsername(adminUsername)) {
            return;
        }

        final var account = AccountEntity.builder()
                .username(adminUsername)
                .password(this.passwordEncoder.encode(adminPassword))
                .firstName("Mr.")
                .lastName("Owner")
                .status(AccountStatus.ACTIVE)
                .mustChangePassword(false)
                .failedLoginAttempts(0)
                .build();
        this.accountRepository.save(account);
        saveAccountRoleMap(account, roleEntity);
    }

    private void saveAccountRoleMap(AccountEntity account, RoleEntity roleEntity) {
        AccountRoleMapEntity accountRoleMapEntity = AccountRoleMapEntity.builder()
                .accountId(account.getId())
                .roleId(roleEntity.getId())
                .build();
        accountRoleMapRepository.save(accountRoleMapEntity);
    }
}
