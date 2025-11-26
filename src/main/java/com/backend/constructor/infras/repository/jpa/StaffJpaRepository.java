package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.StaffEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffJpaRepository extends BaseJpaRepository<StaffEntity> {
}
