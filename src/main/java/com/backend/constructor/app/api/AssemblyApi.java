package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.assembly.AssemblyDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AssemblyApi {
    @PreAuthorize("hasAnyAuthority('MATERIAL_MANAGE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse create(AssemblyDto input);

    @PreAuthorize("hasAnyAuthority('MATERIAL_MANAGE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse update(AssemblyDto input);

    @PreAuthorize("hasAnyAuthority('MATERIAL_MANAGE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    Page<AssemblyDto> getListAssembly(String search, Pageable pageable);
}
