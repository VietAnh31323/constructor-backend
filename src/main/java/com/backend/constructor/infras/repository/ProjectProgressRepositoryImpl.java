package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectProgressEntity;
import com.backend.constructor.core.port.repository.ProjectProgressRepository;
import com.backend.constructor.infras.repository.jpa.ProjectProgressJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectProgressRepositoryImpl extends JpaRepositoryAdapter<ProjectProgressEntity> implements ProjectProgressRepository {
    private final ProjectProgressJpaRepository projectProgressJpaRepository;

    @Override
    public List<ProjectProgressEntity> getListByProjectId(Long projectId) {
        return projectProgressJpaRepository.findAllByProjectId(projectId);
    }
}
