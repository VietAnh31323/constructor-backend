package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectMemberEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberJpaRepository extends BaseJpaRepository<ProjectMemberEntity> {
    List<ProjectMemberEntity> findAllByProjectIdAndDeletedAtIsNull(Long projectId);

    List<ProjectMemberEntity> findAllByStaffIdAndDeletedAtIsNull(Long staffId);

    List<ProjectMemberEntity> findAllByStaffIdInAndDeletedAtIsNull(Collection<Long> staffIds);

    List<ProjectMemberEntity> findAllByProjectIdInAndDeletedAtIsNull(Collection<Long> projectIds);

    Optional<ProjectMemberEntity> findByProjectIdAndStaffIdAndDeletedAtIsNull(Long projectId, Long staffId);

    void deleteAllByProjectId(Long projectId);
}
