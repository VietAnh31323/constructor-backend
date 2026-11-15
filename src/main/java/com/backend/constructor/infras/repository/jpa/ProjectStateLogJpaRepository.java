package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectStateLogEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectStateLogJpaRepository extends BaseJpaRepository<ProjectStateLogEntity> {
    List<ProjectStateLogEntity> findAllByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);
}
