package com.backend.constructor.config.init;

import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.user.entity.AccountEntity;
import com.backend.constructor.user.repository.AccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountInit implements CommandLineRunner {
    AccountRepository accountRepository;
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Init admin");
        if (accountRepository.count() > 0) {
            return;
        }


        String adminUsername = "admin";
        String adminPassword = "admin";
        String displayName = "owner";
        final var account = AccountEntity.builder()
                .username(adminUsername)
                .passwordHash(this.passwordEncoder.encode(adminPassword))
                .displayName(displayName)
                .status(AccountStatus.ACTIVE)
//                .roles(List.of(
//                        RoleEntity.builder()
//                                .name(ERole.ADMIN)
//                                .build(),
//                        RoleEntity.builder()
//                                .name(ERole.USER)
//                                .build()
//                ))
                .build();
        this.accountRepository.save(account);
    }
}
