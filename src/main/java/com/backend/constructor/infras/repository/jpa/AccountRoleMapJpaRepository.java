package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRoleMapJpaRepository extends BaseJpaRepository<AccountRoleMapEntity> {
}
