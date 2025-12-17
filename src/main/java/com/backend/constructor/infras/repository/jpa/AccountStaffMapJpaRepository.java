package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountStaffMapJpaRepository extends BaseJpaRepository<AccountStaffMapEntity> {
}
