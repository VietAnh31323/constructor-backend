package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.SteelCategoryLineEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelCategoryLineJpaRepository extends BaseJpaRepository<SteelCategoryLineEntity> {
    List<SteelCategoryLineEntity> findAllBySteelCategoryId(Long steelCategoryId);
}
