package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.steel.SteelDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SteelApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(SteelDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(SteelDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    SteelDto getDetail(Long id);
}
