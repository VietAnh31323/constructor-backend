package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.steel.SteelDto;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface SteelApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(SteelDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(SteelDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    SteelDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    List<SteelOutput> getListSteelByAssemblyId(Long assemblyId);
}
