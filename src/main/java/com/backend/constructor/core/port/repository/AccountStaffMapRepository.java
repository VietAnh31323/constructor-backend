package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountStaffMapRepository extends BaseRepository<AccountStaffMapEntity> {
    List<AccountStaffMapEntity> getListByStaffId(Long staffId);

    List<AccountStaffMapEntity> getListByStaffIds(Collection<Long> staffIds);

    Optional<AccountStaffMapEntity> findOptionalByAccountId(Long accountId);

    void deleteAllInBatch(List<AccountStaffMapEntity> accountStaffMapEntities);
}
