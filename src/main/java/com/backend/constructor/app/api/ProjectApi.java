package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.project.*;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.core.domain.enums.ProjectDocumentFolder;
import com.backend.constructor.core.domain.enums.ProjectPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ProjectApi {
    @PreAuthorize("hasAnyAuthority('PROJECT_CREATE', 'SCOPE_ADMIN')")
    IdResponse create(ProjectDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse update(ProjectDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_DELETE', 'SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    ProjectDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    Page<ProjectOutput> getListProject(ProjectFilterParam param,
                                       Pageable pageable);

    // --- 2.09: Mở rộng vòng đời dự án, kiểm tra điều kiện & ghi log ---
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    ProjectLifecycleSummaryDto getLifecycleSummary();

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse transitionState(Long projectId, ProjectStateTransitionDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    List<ProjectStateLogDto> getStateLogs(Long projectId);

    // --- Biểu đồ Gantt & Tiến độ dự án ---
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'PROGRESS_VIEW', 'TASK_VIEW', 'SCOPE_ADMIN')")
    ProjectGanttDto getProjectGantt(Long projectId);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PROGRESS_UPDATE', 'TASK_CREATE', 'SCOPE_ADMIN')")
    IdResponse addGanttPhase(Long projectId, ProjectGanttPhaseRequestDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PROGRESS_UPDATE', 'SCOPE_ADMIN')")
    void deleteGanttPhase(Long projectId, Long projectProgressId);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PROGRESS_UPDATE', 'TASK_CREATE', 'SCOPE_ADMIN')")
    IdResponse addGanttTask(Long projectId, ProjectGanttTaskRequestDto input);

    // ==================== 2.10: THÀNH VIÊN DỰ ÁN & PHÂN QUYỀN DỮ LIỆU ====================
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    List<ProjectMemberDto> getProjectMembers(Long projectId);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse addProjectMember(Long projectId, ProjectMemberDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse updateProjectMember(Long projectId, Long memberId, ProjectMemberDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    void removeProjectMember(Long projectId, Long memberId);

    // ==================== 2.11: HỢP ĐỒNG & PHỤ LỤC HỢP ĐỒNG ====================
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    List<ContractDto> getProjectContracts(Long projectId);

    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    ContractDto getContractDetail(Long projectId, Long contractId);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse createContract(Long projectId, ContractDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse updateContract(Long projectId, Long contractId, ContractDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    void deleteContract(Long projectId, Long contractId);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse addContractAddendum(Long projectId, Long contractId, ContractAddendumDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    IdResponse updateContractAddendum(Long projectId, Long contractId, Long addendumId, ContractAddendumDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    void deleteContractAddendum(Long projectId, Long contractId, Long addendumId);

    // ==================== 2.13 & 2.14: KẾ HOẠCH ĐỢT THANH TOÁN & GHI NHẬN THU TIỀN THỰC TẾ ====================
    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PAYMENT_CREATE', 'FINANCE_MANAGE', 'SCOPE_ADMIN')")
    List<ProjectLineDto> savePaymentPlans(Long projectId, List<ProjectLineDto> lines);

    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'PAYMENT_VIEW', 'FINANCE_MANAGE', 'SCOPE_ADMIN')")
    ProjectPaymentSummaryDto getProjectPayments(Long projectId);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PAYMENT_CREATE', 'FINANCE_MANAGE', 'SCOPE_ADMIN')")
    ProjectPaymentDto createProjectPayment(Long projectId, ProjectPaymentDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PAYMENT_CREATE', 'PAYMENT_APPROVE', 'FINANCE_MANAGE', 'SCOPE_ADMIN')")
    ProjectPaymentDto updateProjectPayment(Long projectId, Long paymentId, ProjectPaymentDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PAYMENT_APPROVE', 'FINANCE_MANAGE', 'SCOPE_ADMIN')")
    ProjectPaymentDto updateProjectPaymentStatus(Long projectId, Long paymentId, ProjectPaymentStatus status);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'PAYMENT_APPROVE', 'FINANCE_MANAGE', 'SCOPE_ADMIN')")
    void deleteProjectPayment(Long projectId, Long paymentId);

    // ==================== 2.15: QUẢN LÝ TÀI LIỆU DỰ ÁN ====================
    @PreAuthorize("hasAnyAuthority('PROJECT_VIEW', 'SCOPE_ADMIN')")
    List<ProjectDocumentDto> getProjectDocuments(Long projectId, ProjectDocumentFolder folder);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    ProjectDocumentDto addProjectDocument(Long projectId, ProjectDocumentDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    ProjectDocumentDto updateProjectDocument(Long projectId, Long documentId, ProjectDocumentDto input);

    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    void deleteProjectDocument(Long projectId, Long documentId);

    // ==================== 2.19: KHỐI LƯỢNG & ĐƠN GIÁ HẠNG MỤC ====================
    @PreAuthorize("hasAnyAuthority('PROJECT_UPDATE', 'SCOPE_ADMIN')")
    List<ProjectCategoryMapDto> saveProjectCategories(Long projectId, List<ProjectCategoryMapDto> categories);
}
