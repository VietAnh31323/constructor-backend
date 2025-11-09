package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.AccountEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountJpaRepository extends BaseJpaRepository<AccountEntity> {
    Optional<AccountEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
