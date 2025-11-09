package com.backend.constructor.common.base.repository;

import com.backend.constructor.common.base.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseJpaRepository<E extends BaseEntity> extends JpaRepository<E, Long> {
}
