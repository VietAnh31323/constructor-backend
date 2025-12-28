package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.steel_category.SteelCategoryFilterParam;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.SteelCategoryEntity;
import com.backend.constructor.core.domain.entity.SteelCategoryEntity_;
import com.backend.constructor.core.port.repository.SteelCategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SteelCategoryRepositoryImpl extends JpaRepositoryAdapter<SteelCategoryEntity> implements SteelCategoryRepository {
    private final EntityManager entityManager;

    @Override
    public SteelCategoryEntity getSteelCategoryById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<SteelCategoryEntity> getPageSteelCategory(SteelCategoryFilterParam param,
                                                          Pageable pageable) {
        String search = StringUtils.trimToNull(param.getSearch());
        Filter<SteelCategoryEntity> filter = Filter.builder()
                .search()
                .isContains(SteelCategoryEntity_.CODE, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(SteelCategoryEntity_.NAME, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .filter()
                .isEqual(SteelCategoryEntity_.IS_ACTIVE, param.getIsActive())
                .pageable(pageable)
                .withContext(entityManager)
                .build(SteelCategoryEntity.class);
        return filter.getPage();
    }
}
