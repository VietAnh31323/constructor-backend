package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import com.backend.constructor.core.port.repository.AccountRoleMapRepository;
import com.backend.constructor.infras.repository.jpa.AccountRoleMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountRoleMapRepositoryImpl extends JpaRepositoryAdapter<AccountRoleMapEntity> implements AccountRoleMapRepository {
    private final AccountRoleMapJpaRepository accountRoleMapJpaRepository;

    @Override
    public List<AccountRoleMapEntity> getListByAccountId(Long accountId) {
        return accountRoleMapJpaRepository.findByAccountId(accountId);
    }

    @Override
    public void deleteAllInBatch(List<AccountRoleMapEntity> accountRoleMapEntities) {
        accountRoleMapJpaRepository.deleteAllInBatch(accountRoleMapEntities);
    }
}
