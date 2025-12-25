package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.port.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl extends JpaRepositoryAdapter<ProjectEntity> implements ProjectRepository {
    @Override
    public ProjectEntity getProjectById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }
}
