package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.core.domain.entity.RoleEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<RoleEntity> {
    List<RoleEntity> getListRoleByAccountId(Long accountId);

    Optional<RoleEntity> getByName(ERole eRole);
}