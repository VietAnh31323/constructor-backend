package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectStateLogEntity;
import com.backend.constructor.core.port.repository.ProjectStateLogRepository;
import com.backend.constructor.infras.repository.jpa.ProjectStateLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectStateLogRepositoryImpl extends JpaRepositoryAdapter<ProjectStateLogEntity> implements ProjectStateLogRepository {
    private final ProjectStateLogJpaRepository projectStateLogJpaRepository;

    @Override
    public List<ProjectStateLogEntity> getListByProjectId(Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        return projectStateLogJpaRepository.findAllByProjectIdOrderByCreatedAtDescIdDesc(projectId);
    }
}
