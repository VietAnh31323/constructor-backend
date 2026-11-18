package com.backend.constructor.app.rest;

import com.backend.constructor.app.dto.task.TaskCommentDto;
import com.backend.constructor.app.dto.task.TaskSiteReportDto;
import com.backend.constructor.core.service.TaskCollaborationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 2.25: Báo cáo hiện trường – 2.27: Bình luận & đính kèm trong công việc
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TaskCollaborationController {
    private final TaskCollaborationService collaborationService;

    // ---------- 2.25: Báo cáo hiện trường ----------

    @Operation(summary = "Danh sách báo cáo hiện trường của công việc")
    @GetMapping("/task/{taskId}/site-reports")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'PROGRESS_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public List<TaskSiteReportDto> getSiteReports(@PathVariable Long taskId) {
        return collaborationService.getSiteReports(taskId);
    }

    @Operation(summary = "Danh sách báo cáo hiện trường của toàn dự án")
    @GetMapping("/project/{projectId}/site-reports")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'PROGRESS_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public List<TaskSiteReportDto> getProjectSiteReports(@PathVariable Long projectId) {
        return collaborationService.getProjectSiteReports(projectId);
    }

    @Operation(summary = "Gửi báo cáo hiện trường (ảnh trước/trong/sau, ghi chú, GPS)")
    @PostMapping("/task/{taskId}/site-reports")
    @PreAuthorize("hasAnyAuthority('TASK_UPDATE', 'PROGRESS_UPDATE', 'SITE_ENGINEER', 'SCOPE_ADMIN')")
    public TaskSiteReportDto createSiteReport(@PathVariable Long taskId,
                                              @RequestBody @Valid TaskSiteReportDto input) {
        return collaborationService.createSiteReport(taskId, input);
    }

    @DeleteMapping("/task/{taskId}/site-reports/{reportId}")
    @PreAuthorize("hasAnyAuthority('TASK_UPDATE', 'PROGRESS_UPDATE', 'SITE_ENGINEER', 'SCOPE_ADMIN')")
    public void deleteSiteReport(@PathVariable Long taskId, @PathVariable Long reportId) {
        collaborationService.deleteSiteReport(taskId, reportId);
    }

    // ---------- 2.27: Bình luận ----------

    @GetMapping("/task/{taskId}/comments")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'PROGRESS_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public List<TaskCommentDto> getComments(@PathVariable Long taskId) {
        return collaborationService.getComments(taskId);
    }

    @PostMapping("/task/{taskId}/comments")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'PROGRESS_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public TaskCommentDto createComment(@PathVariable Long taskId,
                                        @RequestBody TaskCommentDto input) {
        return collaborationService.createComment(taskId, input);
    }

    @PutMapping("/task/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'PROGRESS_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public TaskCommentDto updateComment(@PathVariable Long taskId,
                                        @PathVariable Long commentId,
                                        @RequestBody TaskCommentDto input) {
        return collaborationService.updateComment(taskId, commentId, input);
    }

    @DeleteMapping("/task/{taskId}/comments/{commentId}")
    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'PROGRESS_VIEW', 'PROJECT_VIEW', 'SCOPE_ADMIN')")
    public void deleteComment(@PathVariable Long taskId, @PathVariable Long commentId) {
        collaborationService.deleteComment(taskId, commentId);
    }
}
