package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends BaseRepository<ProjectEntity> {
    ProjectEntity getProjectById(Long id);
}