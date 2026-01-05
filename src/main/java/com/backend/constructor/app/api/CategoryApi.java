package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.category.CategoryDto;
import com.backend.constructor.app.dto.category.CategoryFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CategoryApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(CategoryDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(CategoryDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse delete(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    CategoryDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    Page<CategoryDto> getListCategory(CategoryFilterParam param,
                                      Pageable pageable);
}
