package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectProgressTaskMapEntity;
import com.backend.constructor.core.port.repository.ProjectProgressTaskMapRepository;
import com.backend.constructor.infras.repository.jpa.ProjectProgressTaskMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectProgressTaskMapRepositoryImpl extends JpaRepositoryAdapter<ProjectProgressTaskMapEntity> implements ProjectProgressTaskMapRepository {
    private final ProjectProgressTaskMapJpaRepository projectProgressTaskMapJpaRepository;

    @Override
    public List<ProjectProgressTaskMapEntity> getListByProjectProgressIds(Collection<Long> projectProgressIds) {
        if (CollectionUtils.isEmpty(projectProgressIds)) {
            return Collections.emptyList();
        }
        return projectProgressTaskMapJpaRepository.findAllByProjectProgressIdIn(projectProgressIds);
    }

    @Override
    public List<ProjectProgressTaskMapEntity> getListByTaskIds(Collection<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyList();
        }
        return projectProgressTaskMapJpaRepository.findAllByTaskIdIn(taskIds);
    }
}
