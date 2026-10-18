package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.app.dto.steel_project.SteelProjectOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SteelProjectApi {
    @PreAuthorize("hasAnyAuthority('MATERIAL_MANAGE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse create(SteelProjectDto input);

    @PreAuthorize("hasAnyAuthority('MATERIAL_MANAGE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse update(SteelProjectDto input);

    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    SteelProjectDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('MATERIAL_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    Page<SteelProjectOutput> getListSteelProject(String search, Pageable pageable);
}
