package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectProgressTaskMapEntity;
import com.backend.constructor.core.port.repository.ProjectProgressTaskMapRepository;
import com.backend.constructor.infras.repository.jpa.ProjectProgressTaskMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectProgressTaskMapRepositoryImpl extends JpaRepositoryAdapter<ProjectProgressTaskMapEntity> implements ProjectProgressTaskMapRepository {
    private final ProjectProgressTaskMapJpaRepository projectProgressTaskMapJpaRepository;

    @Override
    public List<ProjectProgressTaskMapEntity> getListByProjectProgressIds(Collection<Long> projectProgressIds) {
        return projectProgressTaskMapJpaRepository.findAllByProjectProgressIdIn(projectProgressIds);
    }
}
