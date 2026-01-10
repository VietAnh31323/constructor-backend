package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AssemblyEntity;
import com.backend.constructor.core.domain.entity.AssemblyEntity_;
import com.backend.constructor.core.port.repository.AssemblyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AssemblyRepositoryImpl extends JpaRepositoryAdapter<AssemblyEntity> implements AssemblyRepository {
    private final EntityManager entityManager;

    @Override
    public AssemblyEntity getAssemblyById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public Page<AssemblyEntity> getPageAssembly(String search, Pageable pageable) {
        Filter<AssemblyEntity> filter = Filter.builder()
                .search()
                .isContains(AssemblyEntity_.NAME, StringUtils.trimToNull(search), FilterFlag.UNACCENT_CASE_SENSITIVE)
                .pageable(pageable)
                .withContext(entityManager)
                .build(AssemblyEntity.class);
        return filter.getPage();
    }
}
