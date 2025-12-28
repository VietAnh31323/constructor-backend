package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.steel_category.SteelCategoryDto;
import com.backend.constructor.app.dto.steel_category.SteelCategoryFilterParam;
import com.backend.constructor.app.dto.steel_category.SteelCategoryOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SteelCategoryApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(SteelCategoryDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(SteelCategoryDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    SteelCategoryDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    Page<SteelCategoryOutput> getListSteelCategory(SteelCategoryFilterParam param,
                                                   Pageable pageable);
}
