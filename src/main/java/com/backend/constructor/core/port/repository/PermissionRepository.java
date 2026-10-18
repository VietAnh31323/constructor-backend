package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.PermissionEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends BaseRepository<PermissionEntity> {
    Optional<PermissionEntity> getByName(String name);

    List<PermissionEntity> getAllPermissions();

    List<PermissionEntity> getPermissionsByRoleId(Long roleId);

    Set<String> getPermissionNamesByRoleIds(Collection<Long> roleIds);

    Set<String> getPermissionNamesByAccountId(Long accountId);

    List<PermissionEntity> getPermissionsByAccountId(Long accountId);
}
