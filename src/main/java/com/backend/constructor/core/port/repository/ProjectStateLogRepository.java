package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectStateLogEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectStateLogRepository extends BaseRepository<ProjectStateLogEntity> {
    List<ProjectStateLogEntity> getListByProjectId(Long projectId);
}
