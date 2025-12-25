package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectCategoryMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCategoryMapJpaRepository extends BaseJpaRepository<ProjectCategoryMapEntity> {
    List<ProjectCategoryMapEntity> findAllByProjectId(Long projectId);
}
