package com.backend.constructor.config.init;

import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import com.backend.constructor.core.domain.entity.RoleEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.core.port.repository.AccountRoleMapRepository;
import com.backend.constructor.core.port.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountInit implements CommandLineRunner {
    AccountRepository accountRepository;
    PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AccountRoleMapRepository accountRoleMapRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Init admin");
        RoleEntity roleEntity = roleRepository.getByName(ERole.ADMIN)
                .orElseGet(() -> {
                    RoleEntity adminRoleEntity = RoleEntity.builder()
                            .name(ERole.ADMIN)
                            .description("Admin")
                            .build();
                    return roleRepository.save(adminRoleEntity);
                });
        String adminUsername = "admin";
        String adminPassword = "admin";
        if (accountRepository.existsByUsername(adminUsername)) {
            return;
        }

        final var account = AccountEntity.builder()
                .username(adminUsername)
                .password(this.passwordEncoder.encode(adminPassword))
                .firstName("Mr.")
                .lastName("Owner")
                .status(AccountStatus.ACTIVE)
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
