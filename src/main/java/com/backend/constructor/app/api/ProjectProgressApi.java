package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.project_progress.PhaseReorderDto;
import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.app.dto.project_progress.ProjectPhaseDto;
import com.backend.constructor.app.dto.project_progress.ProjectProgressSummaryDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;

public interface ProjectProgressApi {
    @PreAuthorize("hasAnyAuthority('PROGRESS_UPDATE', 'PROJECT_UPDATE', 'TASK_CREATE', 'SCOPE_ADMIN')")
    IdResponse create(ProjectInfoDto input);

    @PreAuthorize("hasAnyAuthority('PROGRESS_VIEW', 'PROJECT_VIEW', 'TASK_VIEW', 'SCOPE_ADMIN')")
    ProjectInfoDto getDetail(Long id);

    // ---------- 2.21: Quản lý giai đoạn ----------
    @PreAuthorize("hasAnyAuthority('PROGRESS_VIEW', 'PROJECT_VIEW', 'TASK_VIEW', 'SCOPE_ADMIN')")
    List<ProjectPhaseDto> getPhases(Long projectId);

    @PreAuthorize("hasAnyAuthority('PROGRESS_UPDATE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    ProjectPhaseDto updatePhase(Long projectProgressId, ProjectPhaseDto input);

    @PreAuthorize("hasAnyAuthority('PROGRESS_UPDATE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    void deletePhase(Long projectProgressId);

    @PreAuthorize("hasAnyAuthority('PROGRESS_UPDATE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    List<ProjectPhaseDto> reorderPhases(Long projectId, PhaseReorderDto input);

    // ---------- 2.22: Tính lại % tiến độ thủ công (đồng bộ dữ liệu cũ) ----------
    @PreAuthorize("hasAnyAuthority('PROGRESS_UPDATE', 'PROJECT_UPDATE', 'SCOPE_ADMIN')")
    BigDecimal recalculate(Long projectId);

    // ---------- 2.24: Tổng hợp cho màn Quản lý tiến độ ----------
    @PreAuthorize("hasAnyAuthority('PROGRESS_VIEW', 'PROJECT_VIEW', 'TASK_VIEW', 'SCOPE_ADMIN')")
    List<ProjectProgressSummaryDto> getSummaries(List<Long> projectIds);
}
