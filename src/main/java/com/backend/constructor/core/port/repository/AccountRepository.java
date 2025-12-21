package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.AccountEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends BaseRepository<AccountEntity> {
    AccountEntity findByUsernameAndRole(String username, ERole role);

    Optional<AccountEntity> findByUsername(String username);

    AccountEntity getByUsername(String username);

    boolean existsByUsername(String username);

    AccountEntity getAccountById(Long accountId);
}