package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.ProjectApi;
import com.backend.constructor.app.dto.project.*;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import com.backend.constructor.core.domain.enums.ProjectPaymentStatus;
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

    // ==================== 2.13 & 2.14: KẾ HOẠCH ĐỢT THANH TOÁN & GHI NHẬN THU TIỀN THỰC TẾ ====================

    @Override
    @PutMapping("/{projectId}/payment-plans")
    public List<ProjectLineDto> savePaymentPlans(@PathVariable Long projectId,
                                                 @RequestBody @Valid List<ProjectLineDto> lines) {
        return projectService.savePaymentPlans(projectId, lines);
    }

    @Override
    @GetMapping("/{projectId}/payments")
    public ProjectPaymentSummaryDto getProjectPayments(@PathVariable Long projectId) {
        return projectService.getProjectPayments(projectId);
    }

    @Override
    @PostMapping("/{projectId}/payments")
    public ProjectPaymentDto createProjectPayment(@PathVariable Long projectId,
                                                  @RequestBody @Valid ProjectPaymentDto input) {
        return projectService.createProjectPayment(projectId, input);
    }

    @Override
    @PutMapping("/{projectId}/payments/{paymentId}")
    public ProjectPaymentDto updateProjectPayment(@PathVariable Long projectId,
                                                  @PathVariable Long paymentId,
                                                  @RequestBody @Valid ProjectPaymentDto input) {
        return projectService.updateProjectPayment(projectId, paymentId, input);
    }

    @Override
    @PatchMapping("/{projectId}/payments/{paymentId}/status")
    public ProjectPaymentDto updateProjectPaymentStatus(@PathVariable Long projectId,
                                                        @PathVariable Long paymentId,
                                                        @RequestParam ProjectPaymentStatus status) {
        return projectService.updateProjectPaymentStatus(projectId, paymentId, status);
    }

    @Override
    @DeleteMapping("/{projectId}/payments/{paymentId}")
    public void deleteProjectPayment(@PathVariable Long projectId,
                                     @PathVariable Long paymentId) {
        projectService.deleteProjectPayment(projectId, paymentId);
    }

    // ==================== 2.15: QUẢN LÝ TÀI LIỆU DỰ ÁN ====================

    @Override
    @GetMapping("/{projectId}/documents")
    public List<ProjectDocumentDto> getProjectDocuments(@PathVariable Long projectId,
                                                        @RequestParam(required = false) ProjectDocumentFolder folder) {
        return projectService.getProjectDocuments(projectId, folder);
    }

    @Override
    @PostMapping("/{projectId}/documents")
    public ProjectDocumentDto addProjectDocument(@PathVariable Long projectId,
                                                 @RequestBody @Valid ProjectDocumentDto input) {
        return projectService.addProjectDocument(projectId, input);
    }

    @Override
    @PutMapping("/{projectId}/documents/{documentId}")
    public ProjectDocumentDto updateProjectDocument(@PathVariable Long projectId,
                                                    @PathVariable Long documentId,
                                                    @RequestBody @Valid ProjectDocumentDto input) {
        return projectService.updateProjectDocument(projectId, documentId, input);
    }

    @Override
    @DeleteMapping("/{projectId}/documents/{documentId}")
    public void deleteProjectDocument(@PathVariable Long projectId,
                                      @PathVariable Long documentId) {
        projectService.deleteProjectDocument(projectId, documentId);
    }

    // ==================== 2.19: KHỐI LƯỢNG & ĐƠN GIÁ HẠNG MỤC ====================

    @Override
    @PutMapping("/{projectId}/categories")
    public List<ProjectCategoryMapDto> saveProjectCategories(@PathVariable Long projectId,
                                                             @RequestBody @Valid List<ProjectCategoryMapDto> categories) {
        return projectService.saveProjectCategories(projectId, categories);
    }
}
