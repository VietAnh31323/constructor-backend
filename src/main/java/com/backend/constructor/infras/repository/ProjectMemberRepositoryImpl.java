package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectMemberEntity;
import com.backend.constructor.core.port.repository.ProjectMemberRepository;
import com.backend.constructor.infras.repository.jpa.ProjectMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProjectMemberRepositoryImpl extends JpaRepositoryAdapter<ProjectMemberEntity> implements ProjectMemberRepository {
    private final ProjectMemberJpaRepository projectMemberJpaRepository;

    @Override
    public List<ProjectMemberEntity> getListByProjectId(Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        return projectMemberJpaRepository.findAllByProjectIdAndDeletedAtIsNull(projectId);
    }

    @Override
    public List<ProjectMemberEntity> getListByStaffId(Long staffId) {
        if (staffId == null) {
            return Collections.emptyList();
        }
        return projectMemberJpaRepository.findAllByStaffIdAndDeletedAtIsNull(staffId);
    }

    @Override
    public List<ProjectMemberEntity> getListByStaffIds(Collection<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return Collections.emptyList();
        }
        return projectMemberJpaRepository.findAllByStaffIdInAndDeletedAtIsNull(staffIds);
    }

    @Override
    public List<ProjectMemberEntity> getListByProjectIds(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return projectMemberJpaRepository.findAllByProjectIdInAndDeletedAtIsNull(projectIds);
    }

    @Override
    public Optional<ProjectMemberEntity> findByProjectIdAndStaffId(Long projectId, Long staffId) {
        if (projectId == null || staffId == null) {
            return Optional.empty();
        }
        return projectMemberJpaRepository.findByProjectIdAndStaffIdAndDeletedAtIsNull(projectId, staffId);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        if (projectId != null) {
            projectMemberJpaRepository.deleteAllByProjectId(projectId);
        }
    }
}
