package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectCategoryMapEntity;
import com.backend.constructor.core.port.repository.ProjectCategoryMapRepository;
import com.backend.constructor.infras.repository.jpa.ProjectCategoryMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectCategoryMapRepositoryImpl extends JpaRepositoryAdapter<ProjectCategoryMapEntity> implements ProjectCategoryMapRepository {
    private final ProjectCategoryMapJpaRepository projectCategoryMapJpaRepository;

    @Override
    public List<ProjectCategoryMapEntity> getListByProjectId(Long projectId) {
        return projectCategoryMapJpaRepository.findAllByProjectId(projectId);
    }

    @Override
    public void deleteAllInBatch(List<ProjectCategoryMapEntity> projectCategoryMapEntities) {
        projectCategoryMapJpaRepository.deleteAllInBatch(projectCategoryMapEntities);
    }
}
