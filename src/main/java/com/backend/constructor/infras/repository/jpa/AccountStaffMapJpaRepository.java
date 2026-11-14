package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountStaffMapJpaRepository extends BaseJpaRepository<AccountStaffMapEntity> {
    List<AccountStaffMapEntity> findAllByStaffIdIn(Collection<Long> staffIds);

    Optional<AccountStaffMapEntity> findFirstByAccountId(Long accountId);
}
