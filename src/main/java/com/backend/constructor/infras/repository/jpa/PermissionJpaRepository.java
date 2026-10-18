package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.PermissionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionJpaRepository extends BaseJpaRepository<PermissionEntity> {
    Optional<PermissionEntity> findByName(String name);

    List<PermissionEntity> findByModule(String module);
}
