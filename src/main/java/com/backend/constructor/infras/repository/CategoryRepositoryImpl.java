package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.CategoryEntity;
import com.backend.constructor.core.domain.entity.CategoryEntity_;
import com.backend.constructor.core.port.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl extends JpaRepositoryAdapter<CategoryEntity> implements CategoryRepository {
    private final EntityManager entityManager;

    @Override
    public CategoryEntity getCategoryById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<CategoryEntity> getPageCategory(CategoryFilterParam param, Pageable pageable) {
        String trimSearch = StringUtils.trimToNull(param.getSearch());
        Filter<CategoryEntity> filter = Filter.builder()
                .search()
                .isContains(CategoryEntity_.NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .filter()
                .isEqual(CategoryEntity_.IS_ACTIVE, param.getIsActive())
                .pageable(pageable)
                .withContext(entityManager)
                .build(CategoryEntity.class);
        return filter.getPage();
    }

    @Override
    public Map<Long, CodeNameResponse> getMapSimpleCategoryByIds(Set<Long> categoryIds) {
        return findAllByIds(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, entity -> CodeNameResponse.builder()
                        .id(entity.getId())
                        .code(entity.getCode())
                        .name(entity.getName())
                        .build()));
    }
}
