package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity;
import com.backend.constructor.core.domain.entity.AccountStaffMapEntity_;
import com.backend.constructor.core.port.repository.AccountStaffMapRepository;
import com.backend.constructor.infras.repository.jpa.AccountStaffMapJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountStaffMapRepositoryImpl extends JpaRepositoryAdapter<AccountStaffMapEntity> implements AccountStaffMapRepository {
    private final EntityManager entityManager;
    private final AccountStaffMapJpaRepository accountStaffMapJpaRepository;

    @Override
    public List<AccountStaffMapEntity> getListByStaffId(Long staffId) {
        Filter<AccountStaffMapEntity> filter = Filter.builder()
                .filter()
                .isEqual(AccountStaffMapEntity_.STAFF_ID, staffId)
                .withContext(entityManager)
                .build(AccountStaffMapEntity.class);
        return filter.getList();
    }

    @Override
    public void deleteAllInBatch(List<AccountStaffMapEntity> accountStaffMapEntities) {
        accountStaffMapJpaRepository.deleteAllInBatch(accountStaffMapEntities);
    }
}
