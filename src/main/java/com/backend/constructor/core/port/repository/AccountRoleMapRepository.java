package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRoleMapRepository extends BaseRepository<AccountRoleMapEntity> {
    List<AccountRoleMapEntity> getListByAccountId(Long accountId);

    void deleteAllInBatch(List<AccountRoleMapEntity> accountRoleMapEntities);
}