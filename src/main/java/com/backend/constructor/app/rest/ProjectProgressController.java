package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProjectProgressApi;
import com.backend.constructor.app.dto.project_progress.PhaseReorderDto;
import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.app.dto.project_progress.ProjectPhaseDto;
import com.backend.constructor.app.dto.project_progress.ProjectProgressSummaryDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/project-progress")
@RequiredArgsConstructor
public class ProjectProgressController implements ProjectProgressApi {
    private final ProjectProgressApi projectProgressService;

    @Operation(summary = "Thêm mới tiến độ dự án")
    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid ProjectInfoDto input) {
        return projectProgressService.create(input);
    }

    @Override
    @GetMapping
    public ProjectInfoDto getDetail(@RequestParam Long id) {
        return projectProgressService.getDetail(id);
    }

    // ==================== 2.21: QUẢN LÝ GIAI ĐOẠN ====================

    @Operation(summary = "Danh sách giai đoạn của dự án (đã sắp xếp, kèm kế hoạch / thực tế)")
    @Override
    @GetMapping("/{projectId}/phases")
    public List<ProjectPhaseDto> getPhases(@PathVariable Long projectId) {
        return projectProgressService.getPhases(projectId);
    }

    @Operation(summary = "Sửa giai đoạn: đổi tên, ngày kế hoạch, trọng số, ghi chú")
    @Override
    @PutMapping("/phases/{projectProgressId}")
    public ProjectPhaseDto updatePhase(@PathVariable Long projectProgressId,
                                       @RequestBody @Valid ProjectPhaseDto input) {
        return projectProgressService.updatePhase(projectProgressId, input);
    }

    @Operation(summary = "Xóa giai đoạn (kèm toàn bộ công việc thuộc giai đoạn)")
    @Override
    @DeleteMapping("/phases/{projectProgressId}")
    public void deletePhase(@PathVariable Long projectProgressId) {
        projectProgressService.deletePhase(projectProgressId);
    }

    @Operation(summary = "Sắp xếp lại thứ tự giai đoạn")
    @Override
    @PutMapping("/{projectId}/phases/reorder")
    public List<ProjectPhaseDto> reorderPhases(@PathVariable Long projectId,
                                               @RequestBody @Valid PhaseReorderDto input) {
        return projectProgressService.reorderPhases(projectId, input);
    }

    // ==================== 2.22: TÍNH LẠI % TIẾN ĐỘ ====================

    @Operation(summary = "Tính lại % tiến độ việc cha → giai đoạn → dự án theo trọng số")
    @Override
    @PostMapping("/{projectId}/recalculate")
    public BigDecimal recalculate(@PathVariable Long projectId) {
        return projectProgressService.recalculate(projectId);
    }

    // ==================== 2.24: TỔNG HỢP CHO MÀN QUẢN LÝ TIẾN ĐỘ ====================

    @Operation(summary = "Tổng hợp tiến độ (số việc theo trạng thái, trễ hạn) cho danh sách dự án")
    @Override
    @GetMapping("/summary")
    public List<ProjectProgressSummaryDto> getSummaries(@RequestParam List<Long> projectIds) {
        return projectProgressService.getSummaries(projectIds);
    }
}
