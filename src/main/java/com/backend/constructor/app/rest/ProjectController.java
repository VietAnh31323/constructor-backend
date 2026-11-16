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

    // ==================== 2.10: THÀNH VIÊN DỰ ÁN & PHÂN QUYỀN DỮ LIỆU ====================
    @Override
    @GetMapping("/{projectId}/members")
    public List<ProjectMemberDto> getProjectMembers(@PathVariable Long projectId) {
        return projectService.getProjectMembers(projectId);
    }

    @Override
    @PostMapping("/{projectId}/members")
    public IdResponse addProjectMember(@PathVariable Long projectId,
                                       @RequestBody @Valid ProjectMemberDto input) {
        return projectService.addProjectMember(projectId, input);
    }

    @Override
    @PutMapping("/{projectId}/members/{memberId}")
    public IdResponse updateProjectMember(@PathVariable Long projectId,
                                          @PathVariable Long memberId,
                                          @RequestBody @Valid ProjectMemberDto input) {
        return projectService.updateProjectMember(projectId, memberId, input);
    }

    @Override
    @DeleteMapping("/{projectId}/members/{memberId}")
    public void removeProjectMember(@PathVariable Long projectId,
                                    @PathVariable Long memberId) {
        projectService.removeProjectMember(projectId, memberId);
    }

    // ==================== 2.11: HỢP ĐỒNG & PHỤ LỤC HỢP ĐỒNG ====================
    @Override
    @GetMapping("/{projectId}/contracts")
    public List<ContractDto> getProjectContracts(@PathVariable Long projectId) {
        return projectService.getProjectContracts(projectId);
    }

    @Override
    @GetMapping("/{projectId}/contracts/{contractId}")
    public ContractDto getContractDetail(@PathVariable Long projectId,
                                         @PathVariable Long contractId) {
        return projectService.getContractDetail(projectId, contractId);
    }

    @Override
    @PostMapping("/{projectId}/contracts")
    public IdResponse createContract(@PathVariable Long projectId,
                                     @RequestBody @Valid ContractDto input) {
        return projectService.createContract(projectId, input);
    }

    @Override
    @PutMapping("/{projectId}/contracts/{contractId}")
    public IdResponse updateContract(@PathVariable Long projectId,
                                     @PathVariable Long contractId,
                                     @RequestBody @Valid ContractDto input) {
        return projectService.updateContract(projectId, contractId, input);
    }

    @Override
    @DeleteMapping("/{projectId}/contracts/{contractId}")
    public void deleteContract(@PathVariable Long projectId,
                               @PathVariable Long contractId) {
        projectService.deleteContract(projectId, contractId);
    }

    @Override
    @PostMapping("/{projectId}/contracts/{contractId}/addendums")
    public IdResponse addContractAddendum(@PathVariable Long projectId,
                                          @PathVariable Long contractId,
                                          @RequestBody @Valid ContractAddendumDto input) {
        return projectService.addContractAddendum(projectId, contractId, input);
    }

    @Override
    @PutMapping("/{projectId}/contracts/{contractId}/addendums/{addendumId}")
    public IdResponse updateContractAddendum(@PathVariable Long projectId,
                                             @PathVariable Long contractId,
                                             @PathVariable Long addendumId,
                                             @RequestBody @Valid ContractAddendumDto input) {
        return projectService.updateContractAddendum(projectId, contractId, addendumId, input);
    }

    @Override
    @DeleteMapping("/{projectId}/contracts/{contractId}/addendums/{addendumId}")
    public void deleteContractAddendum(@PathVariable Long projectId,
                                       @PathVariable Long contractId,
                                       @PathVariable Long addendumId) {
        projectService.deleteContractAddendum(projectId, contractId, addendumId);
    }
}
