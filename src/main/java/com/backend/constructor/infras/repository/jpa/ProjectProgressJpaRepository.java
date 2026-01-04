package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectProgressEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectProgressJpaRepository extends BaseJpaRepository<ProjectProgressEntity> {
    List<ProjectProgressEntity> findAllByProjectId(Long projectId);
}
