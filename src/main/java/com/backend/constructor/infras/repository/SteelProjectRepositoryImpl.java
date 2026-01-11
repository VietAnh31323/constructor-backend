package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.SteelProjectEntity;
import com.backend.constructor.core.domain.entity.SteelProjectEntity_;
import com.backend.constructor.core.port.repository.SteelProjectRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SteelProjectRepositoryImpl extends JpaRepositoryAdapter<SteelProjectEntity> implements SteelProjectRepository {
    private final EntityManager entityManager;

    @Override
    public SteelProjectEntity getSteelProjectById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<SteelProjectEntity> getPageSteelProject(String search, Pageable pageable) {
        String trimSearch = StringUtils.trimToNull(search);
        Filter<SteelProjectEntity> filter = Filter.builder()
                .search()
                .isContains(SteelProjectEntity_.CODE, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(SteelProjectEntity_.NAME, trimSearch, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .filter()
                .pageable(pageable)
                .withContext(entityManager)
                .build(SteelProjectEntity.class);
        return filter.getPage();
    }
}
