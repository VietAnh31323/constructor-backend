package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.enums.AccountStatus;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AccountEntity;
import com.backend.constructor.core.domain.entity.AccountEntity_;
import com.backend.constructor.core.port.repository.AccountRepository;
import com.backend.constructor.infras.repository.jpa.AccountJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl extends JpaRepositoryAdapter<AccountEntity> implements AccountRepository {
    private final AccountJpaRepository accountJpaRepository;
    private final EntityManager entityManager;

    @Override
    public AccountEntity findByUsernameAndRole(String username, ERole role) {
        return accountJpaRepository.findByUsernameAndRole(username, role)
                .orElseThrow(() -> BusinessException.exception("CST000"));
    }

    @Override
    public Optional<AccountEntity> findOptionalByUsernameAndRole(String username, ERole role) {
        return accountJpaRepository.findByUsernameAndRole(username, role);
    }

    @Override
    public Optional<AccountEntity> findByUsername(String username) {
        return accountJpaRepository.findByUsernameAndStatus(username, AccountStatus.ACTIVE);
    }

    @Override
    public AccountEntity getByUsername(String username) {
        return accountJpaRepository.findByUsernameAndStatus(username, AccountStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.exception("CST000"));
    }

    @Override
    public boolean existsByUsername(String username) {
        return accountJpaRepository.existsByUsername(username);
    }

    @Override
    public AccountEntity getAccountById(Long accountId) {
        return findById(accountId).orElseThrow(() -> new BusinessException("400", "Account not found"));
    }

    @Override
    public Page<AccountEntity> getPageAccount(String search, AccountStatus accountStatus, Pageable pageable) {
        Filter<AccountEntity> filter = Filter.builder()
                .search()
                .isContains(AccountEntity_.USERNAME, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .filter()
                .isEqual(AccountEntity_.STATUS, accountStatus)
                .withContext(entityManager)
                .pageable(pageable)
                .build(AccountEntity.class);
        return filter.getPage();
    }
}
