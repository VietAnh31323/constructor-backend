package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectDocumentEntity;
import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends BaseRepository<ProjectDocumentEntity> {
    List<ProjectDocumentEntity> getListByProjectId(Long projectId);

    List<ProjectDocumentEntity> getListByProjectIdAndFolder(Long projectId, ProjectDocumentFolder folder);

    void deleteAllByProjectId(Long projectId);
}
