package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.project.ProjectFilterParam;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterBuilder;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.base.repository.filter.FilterJoiner;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.repository.ProjectRepository;
import com.backend.constructor.infras.repository.jpa.ProjectJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl extends JpaRepositoryAdapter<ProjectEntity> implements ProjectRepository {
    private final EntityManager entityManager;
    private final ProjectJpaRepository projectJpaRepository;

    @Override
    public ProjectEntity getProjectById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<ProjectEntity> getPageProject(ProjectFilterParam param, Pageable pageable) {
        String search = StringUtils.trimToNull(param.getSearch());
        FilterBuilder filterBuilder = Filter.builder()
                .search()
                .isContains(ProjectEntity_.CODE, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(ProjectEntity_.NAME, search, FilterFlag.UNACCENT_CASE_SENSITIVE);
        if (param.getCategoryId() != null) {
            filterBuilder
                    .leftJoin(ProjectEntity_.ID, new FilterJoiner(ProjectCategoryMapEntity.class, ProjectCategoryMapEntity_.PROJECT_ID))
                    .isEqual(ProjectCategoryMapEntity_.CATEGORY_ID, param.getCategoryId());
        }
        Filter<ProjectEntity> filter = filterBuilder
                .pageable(pageable)
                .withContext(entityManager)
                .build(ProjectEntity.class);
        return filter.getPage();
    }

    @Override
    public ProjectEntity getProjectByTaskId(Long taskId) {
        Filter<ProjectEntity> filter = Filter.builder()
                .leftJoin(ProjectEntity_.ID, new FilterJoiner(ProjectProgressEntity.class, ProjectProgressEntity_.PROJECT_ID))
                .thenLeftJoin(ProjectProgressEntity_.ID, new FilterJoiner(ProjectProgressTaskMapEntity.class, ProjectProgressTaskMapEntity_.PROJECT_PROGRESS_ID))
                .filter()
                .isEqual(ProjectProgressTaskMapEntity_.TASK_ID, taskId)
                .withContext(entityManager)
                .build(ProjectEntity.class);
        List<ProjectEntity> projectEntities = filter.getList();
        if (CollectionUtils.isEmpty(projectEntities)) {
            return null;
        }
        return projectEntities.getFirst();
    }

    @Override
    public List<ProjectEntity> getListByParticipantStaffId(Long staffId) {
        if (staffId == null) {
            return Collections.emptyList();
        }
        return projectJpaRepository.findAllByParticipantStaffId(staffId);
    }

    @Override
    public List<ProjectEntity> getListByParticipantStaffIds(Collection<Long> staffIds) {
        if (CollectionUtils.isEmpty(staffIds)) {
            return Collections.emptyList();
        }
        return projectJpaRepository.findAllByParticipantStaffIds(staffIds);
    }
}
