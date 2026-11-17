package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectDocumentEntity;
import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import com.backend.constructor.core.port.repository.ProjectDocumentRepository;
import com.backend.constructor.infras.repository.jpa.ProjectDocumentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectDocumentRepositoryImpl extends JpaRepositoryAdapter<ProjectDocumentEntity> implements ProjectDocumentRepository {
    private final ProjectDocumentJpaRepository projectDocumentJpaRepository;

    @Override
    public List<ProjectDocumentEntity> getListByProjectId(Long projectId) {
        return projectDocumentJpaRepository.findAllByProjectIdOrderByUploadedAtDescIdDesc(projectId);
    }

    @Override
    public List<ProjectDocumentEntity> getListByProjectIdAndFolder(Long projectId, ProjectDocumentFolder folder) {
        return projectDocumentJpaRepository.findAllByProjectIdAndFolderOrderByUploadedAtDescIdDesc(projectId, folder);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        projectDocumentJpaRepository.deleteAllByProjectId(projectId);
    }
}
