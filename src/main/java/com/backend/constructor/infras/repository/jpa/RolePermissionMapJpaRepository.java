package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.RolePermissionMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RolePermissionMapJpaRepository extends BaseJpaRepository<RolePermissionMapEntity> {
    List<RolePermissionMapEntity> findByRoleId(Long roleId);

    List<RolePermissionMapEntity> findByRoleIdIn(Collection<Long> roleIds);

    void deleteByRoleId(Long roleId);
}
