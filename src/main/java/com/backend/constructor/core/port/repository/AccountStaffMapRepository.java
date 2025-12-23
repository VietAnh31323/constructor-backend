package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountStaffMapRepository extends BaseRepository<AccountStaffMapEntity> {
    List<AccountStaffMapEntity> getListByStaffId(Long staffId);

    void deleteAllInBatch(List<AccountStaffMapEntity> accountStaffMapEntities);
}