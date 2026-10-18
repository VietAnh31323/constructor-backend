package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.RolePermissionMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RolePermissionMapRepository extends BaseRepository<RolePermissionMapEntity> {
    List<RolePermissionMapEntity> getListByRoleId(Long roleId);

    List<RolePermissionMapEntity> getListByRoleIdIn(Collection<Long> roleIds);

    void deleteByRoleId(Long roleId);

    void deleteAllInBatch(Iterable<RolePermissionMapEntity> entities);
}
