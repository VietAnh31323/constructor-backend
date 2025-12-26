package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRoleMapJpaRepository extends BaseJpaRepository<AccountRoleMapEntity> {
    List<AccountRoleMapEntity> findByAccountId(Long accountId);
}
