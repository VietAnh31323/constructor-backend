package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SteelProjectApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(SteelProjectDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(SteelProjectDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    SteelProjectDto getDetail(Long id);
}
