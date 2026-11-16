package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectMemberEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends BaseRepository<ProjectMemberEntity> {
    List<ProjectMemberEntity> getListByProjectId(Long projectId);

    List<ProjectMemberEntity> getListByStaffId(Long staffId);

    List<ProjectMemberEntity> getListByStaffIds(Collection<Long> staffIds);

    List<ProjectMemberEntity> getListByProjectIds(Collection<Long> projectIds);

    Optional<ProjectMemberEntity> findByProjectIdAndStaffId(Long projectId, Long staffId);

    void deleteAllByProjectId(Long projectId);
}
