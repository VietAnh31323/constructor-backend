package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.CategoryEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryJpaRepository extends BaseJpaRepository<CategoryEntity> {
}
