package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.RolePermissionMapEntity;
import com.backend.constructor.core.port.repository.RolePermissionMapRepository;
import com.backend.constructor.infras.repository.jpa.RolePermissionMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RolePermissionMapRepositoryImpl extends JpaRepositoryAdapter<RolePermissionMapEntity> implements RolePermissionMapRepository {
    private final RolePermissionMapJpaRepository rolePermissionMapJpaRepository;

    @Override
    public List<RolePermissionMapEntity> getListByRoleId(Long roleId) {
        return rolePermissionMapJpaRepository.findByRoleId(roleId);
    }

    @Override
    public List<RolePermissionMapEntity> getListByRoleIdIn(Collection<Long> roleIds) {
        return rolePermissionMapJpaRepository.findByRoleIdIn(roleIds);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        rolePermissionMapJpaRepository.deleteByRoleId(roleId);
    }

    @Override
    public void deleteAllInBatch(Iterable<RolePermissionMapEntity> entities) {
        rolePermissionMapJpaRepository.deleteAllInBatch(entities);
    }
}
