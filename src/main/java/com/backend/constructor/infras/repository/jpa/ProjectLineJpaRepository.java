package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLineJpaRepository extends BaseJpaRepository<ProjectLineEntity> {
    List<ProjectLineEntity> findAllByProjectId(Long projectId);
}
