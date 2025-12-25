package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.project.ProjectDto;
import com.backend.constructor.app.dto.project.ProjectFilterParam;
import com.backend.constructor.app.dto.project.ProjectOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProjectApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(ProjectDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(ProjectDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    ProjectDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    Page<ProjectOutput> getListProject(ProjectFilterParam param,
                                       Pageable pageable);
}
