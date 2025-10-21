package com.backend.constructor.user.repository;

import com.backend.constructor.common.enums.ERole;
import com.backend.constructor.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(ERole eRole);
}