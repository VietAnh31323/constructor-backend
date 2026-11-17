package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectDocumentEntity;
import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentJpaRepository extends BaseJpaRepository<ProjectDocumentEntity> {
    List<ProjectDocumentEntity> findAllByProjectIdOrderByUploadedAtDescIdDesc(Long projectId);

    List<ProjectDocumentEntity> findAllByProjectIdAndFolderOrderByUploadedAtDescIdDesc(Long projectId, ProjectDocumentFolder folder);

    void deleteAllByProjectId(Long projectId);
}
