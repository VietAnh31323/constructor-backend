package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.RoleEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleJpaRepository extends BaseJpaRepository<RoleEntity> {
    Optional<RoleEntity> findByName(ERole name);
}
