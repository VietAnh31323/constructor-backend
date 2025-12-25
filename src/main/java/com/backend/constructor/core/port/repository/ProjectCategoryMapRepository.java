package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectCategoryMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCategoryMapRepository extends BaseRepository<ProjectCategoryMapEntity> {
    List<ProjectCategoryMapEntity> getListByProjectId(Long projectId);

    void deleteAllInBatch(List<ProjectCategoryMapEntity> projectCategoryMapEntities);
}