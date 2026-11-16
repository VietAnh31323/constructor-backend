package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.project.ProjectFilterParam;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectRepository extends BaseRepository<ProjectEntity> {
    ProjectEntity getProjectById(Long id);

    Page<ProjectEntity> getPageProject(ProjectFilterParam param,
                                       Pageable pageable);

    Page<ProjectEntity> getPageProject(ProjectFilterParam param,
                                       Pageable pageable,
                                       Collection<Long> scopedProjectIds);

    ProjectEntity getProjectByTaskId(Long taskId);

    List<ProjectEntity> getListByParticipantStaffId(Long staffId);

    List<ProjectEntity> getListByParticipantStaffIds(Collection<Long> staffIds);

    List<ProjectEntity> getListByCustomerId(Long customerId);

    List<ProjectEntity> getListByCustomerIds(Collection<Long> customerIds);

    boolean existsByCustomerId(Long customerId);
}
