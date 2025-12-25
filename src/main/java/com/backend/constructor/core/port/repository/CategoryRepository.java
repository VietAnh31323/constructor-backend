package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;

@Repository
public interface CategoryRepository extends BaseRepository<CategoryEntity> {
    CategoryEntity getCategoryById(Long id);

    Page<CategoryEntity> getPageCategory(CategoryFilterParam param, Pageable pageable);

    Map<Long, CodeNameResponse> getMapSimpleCategoryByIds(Set<Long> categoryIds);
}