package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectProgressTaskMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectProgressTaskMapJpaRepository extends BaseJpaRepository<ProjectProgressTaskMapEntity> {
    List<ProjectProgressTaskMapEntity> findAllByProjectProgressIdIn(Collection<Long> projectProgressIds);
}
