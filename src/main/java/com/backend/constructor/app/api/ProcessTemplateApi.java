package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.process_template.ProcessTemplateDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.enums.BuildingType;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ProcessTemplateApi {
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'PROGRESS_VIEW', 'SCOPE_ADMIN')")
    List<ProcessTemplateDto> getList(BuildingType buildingType);

    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'PROGRESS_VIEW', 'SCOPE_ADMIN')")
    ProcessTemplateDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PROGRESS_UPDATE', 'SCOPE_ADMIN')")
    IdResponse create(ProcessTemplateDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PROGRESS_UPDATE', 'SCOPE_ADMIN')")
    IdResponse update(Long id, ProcessTemplateDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_DELETE', 'PROGRESS_DELETE', 'SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PROGRESS_UPDATE', 'SCOPE_ADMIN')")
    void applyToProject(Long projectId, Long templateId);
}
