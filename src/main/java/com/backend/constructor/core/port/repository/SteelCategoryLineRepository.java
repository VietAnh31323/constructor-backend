package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelCategoryLineEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelCategoryLineRepository extends BaseRepository<SteelCategoryLineEntity> {
    List<SteelCategoryLineEntity> getListByCategoryId(Long steelCategoryId);
}