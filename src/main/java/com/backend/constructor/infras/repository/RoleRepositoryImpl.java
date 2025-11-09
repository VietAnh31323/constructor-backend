package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterJoiner;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity;
import com.backend.constructor.core.domain.entity.AccountRoleMapEntity_;
import com.backend.constructor.core.domain.entity.RoleEntity;
import com.backend.constructor.core.domain.entity.RoleEntity_;
import com.backend.constructor.core.port.repository.RoleRepository;
import com.backend.constructor.infras.repository.jpa.RoleJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl extends JpaRepositoryAdapter<RoleEntity> implements RoleRepository {
    private final EntityManager entityManager;
    private final RoleJpaRepository roleJpaRepository;

    @Override
    public List<RoleEntity> getListRoleByAccountId(Long accountId) {
        Filter<RoleEntity> filter = Filter.builder()
                .filter()
                .leftJoin(RoleEntity_.ID, new FilterJoiner(AccountRoleMapEntity.class, AccountRoleMapEntity_.ROLE_ID))
                .isEqual(AccountRoleMapEntity_.ACCOUNT_ID, accountId)
                .withContext(entityManager)
                .build(RoleEntity.class);
        return filter.getList();
    }

    @Override
    public Optional<RoleEntity> getByName(ERole eRole) {
        return roleJpaRepository.findByName(eRole);
    }
}
