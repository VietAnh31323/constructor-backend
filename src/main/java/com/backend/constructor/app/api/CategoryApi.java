package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.category.CategoryDto;
import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryApi {
    IdResponse create(CategoryDto input);

    IdResponse update(CategoryDto input);

    IdResponse delete(Long id);

    CategoryDto getDetail(Long id);

    Page<CategoryDto> getListStaff(CategoryFilterParam param,
                                   Pageable pageable);
}
