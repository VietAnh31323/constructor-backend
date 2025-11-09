package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.infras.repository.jpa.AccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl extends JpaRepositoryAdapter<AccountEntity> implements AccountRepository {
    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Optional<AccountEntity> findByUsername(String username) {
        return accountJpaRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return accountJpaRepository.existsByUsername(username);
    }

    @Override
    public AccountEntity getAccountById(Long accountId) {
        return findById(accountId).orElseThrow(() -> new BusinessException("400", "Account not found"));
    }
}
