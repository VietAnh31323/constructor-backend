package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectProgressTaskMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectProgressTaskMapRepository extends BaseRepository<ProjectProgressTaskMapEntity> {
    List<ProjectProgressTaskMapEntity> getListByProjectProgressIds(Collection<Long> projectProgressIds);
}