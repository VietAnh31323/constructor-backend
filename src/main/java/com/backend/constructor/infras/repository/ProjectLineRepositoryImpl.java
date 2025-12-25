package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import com.backend.constructor.core.port.repository.ProjectLineRepository;
import com.backend.constructor.infras.repository.jpa.ProjectLineJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectLineRepositoryImpl extends JpaRepositoryAdapter<ProjectLineEntity> implements ProjectLineRepository {
    private final ProjectLineJpaRepository projectLineJpaRepository;

    @Override
    public List<ProjectLineEntity> getListByProjectId(Long projectId) {
        return projectLineJpaRepository.findAllByProjectId(projectId);
    }

    @Override
    public void deleteAllInBatch(List<ProjectLineEntity> projectLineEntities) {
        projectLineJpaRepository.deleteAllInBatch(projectLineEntities);
    }
}
