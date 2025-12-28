package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.steel_category.SteelCategoryFilterParam;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface SteelCategoryRepository extends BaseRepository<SteelCategoryEntity> {
    SteelCategoryEntity getSteelCategoryById(Long id);

    Page<SteelCategoryEntity> getPageSteelCategory(SteelCategoryFilterParam param,
                                                   Pageable pageable);
}