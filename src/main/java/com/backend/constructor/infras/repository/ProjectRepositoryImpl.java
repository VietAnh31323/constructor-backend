package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.project.ProjectFilterParam;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterBuilder;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.base.repository.filter.FilterJoiner;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.ProjectCategoryMapEntity;
import com.backend.constructor.core.domain.entity.ProjectCategoryMapEntity_;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.ProjectEntity_;
import com.backend.constructor.core.port.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl extends JpaRepositoryAdapter<ProjectEntity> implements ProjectRepository {
    private final EntityManager entityManager;

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
                .build(ProjectEntity.class);
        return filter.getPage();
    }
}
