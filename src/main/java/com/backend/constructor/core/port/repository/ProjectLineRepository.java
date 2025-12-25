package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLineRepository extends BaseRepository<ProjectLineEntity> {
    List<ProjectLineEntity> getListByProjectId(Long projectId);

    void deleteAllInBatch(List<ProjectLineEntity> projectLineEntities);
}