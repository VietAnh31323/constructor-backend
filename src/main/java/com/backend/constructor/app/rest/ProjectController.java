package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProjectApi;
import com.backend.constructor.app.dto.project.*;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController implements ProjectApi {
    private final ProjectApi projectService;

    @Override
    @PostMapping
    public IdResponse create(@RequestBody @Valid ProjectDto input) {
        return projectService.create(input);
    }

    @Override
    @PutMapping
    public IdResponse update(@RequestBody @Valid ProjectDto input) {
        return projectService.update(input);
    }

    @Override
    @DeleteMapping
    public void delete(@RequestParam Long id) {
        projectService.delete(id);
    }

    @Override
    @GetMapping
    public ProjectDto getDetail(@RequestParam Long id) {
        return projectService.getDetail(id);
    }

    @Override
    @GetMapping("/list")
    @HandsomePaging
    public Page<ProjectOutput> getListProject(@ParameterObject ProjectFilterParam param,
                                              @ParameterObject Pageable pageable) {
        return projectService.getListProject(param, pageable);
    }

    @Override
    @GetMapping("/lifecycle-summary")
    public ProjectLifecycleSummaryDto getLifecycleSummary() {
        return projectService.getLifecycleSummary();
    }

    @Override
    @PatchMapping("/{projectId}/state")
    public IdResponse transitionState(@PathVariable Long projectId,
                                      @RequestBody @Valid ProjectStateTransitionDto input) {
        return projectService.transitionState(projectId, input);
    }

    @Override
    @GetMapping("/{projectId}/state-logs")
    public List<ProjectStateLogDto> getStateLogs(@PathVariable Long projectId) {
        return projectService.getStateLogs(projectId);
    }

    @Override
    @GetMapping("/{projectId}/gantt")
    public ProjectGanttDto getProjectGantt(@PathVariable Long projectId) {
        return projectService.getProjectGantt(projectId);
    }

    @Override
    @PostMapping("/{projectId}/gantt/phases")
    public IdResponse addGanttPhase(@PathVariable Long projectId,
                                    @RequestBody @Valid ProjectGanttPhaseRequestDto input) {
        return projectService.addGanttPhase(projectId, input);
    }

    @Override
    @DeleteMapping("/{projectId}/gantt/phases/{projectProgressId}")
    public void deleteGanttPhase(@PathVariable Long projectId,
                                 @PathVariable Long projectProgressId) {
        projectService.deleteGanttPhase(projectId, projectProgressId);
    }

    @Override
    @PostMapping("/{projectId}/gantt/tasks")
    public IdResponse addGanttTask(@PathVariable Long projectId,
                                   @RequestBody @Valid ProjectGanttTaskRequestDto input) {
        return projectService.addGanttTask(projectId, input);
    }
}
