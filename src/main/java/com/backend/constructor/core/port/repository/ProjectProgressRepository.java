package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectProgressEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectProgressRepository extends BaseRepository<ProjectProgressEntity> {
    List<ProjectProgressEntity> getListByProjectId(Long projectId);
}