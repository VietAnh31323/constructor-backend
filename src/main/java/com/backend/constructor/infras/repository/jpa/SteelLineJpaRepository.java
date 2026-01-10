package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelLineJpaRepository extends BaseJpaRepository<SteelLineEntity> {
    List<SteelLineEntity> findAllBySteelId(Long steelId);
}
