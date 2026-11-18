package com.backend.constructor.core.service;

import com.backend.constructor.app.api.ProjectApi;
import com.backend.constructor.app.dto.project.*;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.*;
import com.backend.constructor.core.port.mapper.ProjectDocumentMapper;
import com.backend.constructor.core.port.mapper.ProjectLineMapper;
import com.backend.constructor.core.port.mapper.ProjectMemberMapper;
import com.backend.constructor.core.port.mapper.ProjectPaymentMapper;
import com.backend.constructor.core.port.mapper.ContractMapper;
import com.backend.constructor.core.port.mapper.ContractAddendumMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.backend.constructor.core.port.mapper.ProjectMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.user.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.backend.constructor.core.service.HelperService.addIfNotNull;
import static com.backend.constructor.core.service.HelperService.getData;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService implements ProjectApi {
    private final ProjectRepository projectRepository;
    private final ProjectStateLogRepository projectStateLogRepository;
    private final CustomerRepository customerRepository;
    private final ProjectMapper projectMapper;
    private final ProjectLineMapper projectLineMapper;
    private final ProjectLineRepository projectLineRepository;
    private final ProjectCategoryMapRepository projectCategoryMapRepository;
    private final ProjectProgressRepository projectProgressRepository;
    private final ProjectProgressTaskMapRepository projectProgressTaskMapRepository;
    private final ProgressRepository progressRepository;
    private final TaskRepository taskRepository;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final StaffRepository staffRepository;
    private final CategoryRepository categoryRepository;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;
    private final AuditLogService auditLogService;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final ContractAddendumRepository contractAddendumRepository;
    private final ContractAddendumMapper contractAddendumMapper;
    private final AccountRepository accountRepository;
    private final AccountStaffMapRepository accountStaffMapRepository;
    private final ProjectPaymentRepository projectPaymentRepository;
    private final ProjectPaymentMapper projectPaymentMapper;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectDocumentMapper projectDocumentMapper;
    private final ProcessTemplateService processTemplateService;
    // 2.21 – 2.27
    private final ProgressCalculationService progressCalculationService;
    private final TaskWorkflowService taskWorkflowService;
    private final TaskCollaborationService taskCollaborationService;
    private final ProjectProgressService projectProgressService;


    @Override
    @Transactional
    public IdResponse create(ProjectDto input) {
        input.trimData();
        CustomerEntity linkedCustomer = resolveLinkedCustomer(input);

        generateCodeService.generateCode(input, Constants.DA, ProjectEntity.class);
        input.setId(null);
        ProjectEntity projectEntity = projectMapper.toEntity(input);
        applyLinkedCustomerToProject(projectEntity, linkedCustomer, input.getOwner());

        BigDecimal contractVal = projectEntity.getContractValue() != null
                ? projectEntity.getContractValue().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        projectEntity.setContractValue(contractVal);

        if (projectEntity.getContractAdvance() != null) {
            projectEntity.setContractAdvance(projectEntity.getContractAdvance().setScale(2, RoundingMode.HALF_UP));
        }
        projectEntity.setRemainingAmount(calculateRemainingAmount(null, contractVal, projectEntity.getContractAdvance()));

        ProjectState initialState = projectEntity.getState() != null
                ? projectEntity.getState()
                : ProjectState.RECEIVED;
        projectEntity.setState(initialState);
        projectEntity.setStateChangedAt(LocalDateTime.now());
        projectEntity.setStateReason(StringUtils.defaultIfBlank(input.getStateReason(), "Khởi tạo dự án mới"));

        int categoryCount = input.getProjectCategoryMaps() != null ? input.getProjectCategoryMaps().size() : 0;
        validateStatePreConditions(projectEntity, linkedCustomer, null, initialState, projectEntity.getStateReason(), categoryCount);
        applyAutomaticMilestoneDates(projectEntity, initialState, input.getActualHandoverDate(), input.getWarrantyEndDate());

        uniqueValidationService.validate(projectEntity);
        projectRepository.save(projectEntity);
        saveProjectLines(projectEntity, input.getProjectLines());
        saveProjectCategoryMaps(projectEntity, input.getProjectCategoryMaps());

        recordProjectStateLog(projectEntity, null, initialState, projectEntity.getStateReason());
        syncCustomerPipelineOnProjectSave(linkedCustomer, initialState);
        syncProjectMembersOnCreate(projectEntity, input.getMembers());
        processTemplateService.applyDefaultTemplateIfEligible(projectEntity);
        return IdResponse.builder().id(projectEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(ProjectDto input) {
        if (input.getId() == null) {
            throw BusinessException.exception("CST002");
        }
        input.trimData();
        CustomerEntity linkedCustomer = resolveLinkedCustomer(input);

        generateCodeService.generateCode(input, Constants.DA, ProjectEntity.class);
        ProjectEntity projectEntity = projectRepository.getProjectById(input.getId());
        ProjectState existingState = projectEntity.getState() != null
                ? projectEntity.getState()
                : ProjectState.RECEIVED;

        projectMapper.update(input, projectEntity);
        applyLinkedCustomerToProject(projectEntity, linkedCustomer, input.getOwner());

        ProjectState targetState = input.getState() != null ? input.getState() : existingState;
        projectEntity.setState(targetState);

        BigDecimal contractVal = projectEntity.getContractValue() != null
                ? projectEntity.getContractValue().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        projectEntity.setContractValue(contractVal);

        if (projectEntity.getContractAdvance() != null) {
            projectEntity.setContractAdvance(projectEntity.getContractAdvance().setScale(2, RoundingMode.HALF_UP));
        }
        projectEntity.setRemainingAmount(calculateRemainingAmount(projectEntity.getId(), contractVal, projectEntity.getContractAdvance()));

        int categoryCount = input.getProjectCategoryMaps() != null ? input.getProjectCategoryMaps().size() : 0;
        if (targetState != existingState) {
            validateTransitionRule(existingState, targetState, projectEntity.getPreviousState());
            String reason = StringUtils.trimToNull(input.getStateReason());
            validateStatePreConditions(projectEntity, linkedCustomer, existingState, targetState, reason, categoryCount);
            applyStateTransitionMetadata(projectEntity, existingState, targetState, reason,
                    input.getActualHandoverDate(), input.getWarrantyEndDate());
        } else {
            validateStatePreConditions(projectEntity, linkedCustomer, null, targetState, "Cập nhật thông tin", categoryCount);
        }

        uniqueValidationService.validate(projectEntity);
        projectRepository.save(projectEntity);
        List<ProjectCategoryMapEntity> oldCategoryMaps = projectCategoryMapRepository.getListByProjectId(projectEntity.getId());
        if (!oldCategoryMaps.isEmpty()) {
            projectCategoryMapRepository.deleteAllInBatch(oldCategoryMaps);
        }
        saveProjectLines(projectEntity, input.getProjectLines());
        saveProjectCategoryMaps(projectEntity, input.getProjectCategoryMaps());

        if (targetState != existingState) {
            recordProjectStateLog(projectEntity, existingState, targetState, projectEntity.getStateReason());
            auditLogService.logExplicit(
                    "CHANGE_PROJECT_STATE",
                    "PROJECT",
                    "ProjectEntity",
                    String.valueOf(projectEntity.getId()),
                    Map.of("state", existingState.name(), "label", existingState.getLabel()),
                    Map.of(
                            "state", targetState.name(),
                            "label", targetState.getLabel(),
                            "reason", projectEntity.getStateReason() != null ? projectEntity.getStateReason() : ""
                    ),
                    "Chuyển trạng thái dự án " + projectEntity.getCode() + " (" + projectEntity.getName() + "): "
                            + existingState.getLabel() + " → " + targetState.getLabel(),
                    null
            );
        }

        syncCustomerPipelineOnProjectSave(linkedCustomer, targetState);
        syncProjectMembersOnUpdate(projectEntity, input.getMembers());
        return IdResponse.builder().id(projectEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProjectEntity projectEntity = projectRepository.getProjectById(id);
        deleteAllRelations(projectEntity);
        List<ProjectStateLogEntity> logs = projectStateLogRepository.getListByProjectId(id);
        if (!logs.isEmpty()) {
            projectStateLogRepository.deleteAll(logs);
        }
        projectMemberRepository.deleteAllByProjectId(id);
        contractAddendumRepository.deleteAllByProjectId(id);
        contractRepository.deleteAllByProjectId(id);
        projectPaymentRepository.deleteAllByProjectId(id);
        projectDocumentRepository.deleteAllByProjectId(id);
        projectRepository.delete(projectEntity);
    }

    @Override
    public ProjectDto getDetail(Long id) {
        ProjectEntity projectEntity = projectRepository.getProjectById(id);
        ProjectDto projectDto = projectMapper.toDto(projectEntity);
        ProjectState currentState = projectEntity.getState() != null
                ? projectEntity.getState()
                : ProjectState.RECEIVED;
        projectDto.setState(currentState);
        projectDto.setAllowedNextStates(computeAllowedNextStates(currentState, projectEntity.getPreviousState()));
        projectDto.setStateLogCount(projectStateLogRepository.getListByProjectId(id).size());

        if (projectEntity.getCustomerId() != null) {
            customerRepository.findById(projectEntity.getCustomerId()).ifPresent(cust -> {
                projectDto.setCustomerId(cust.getId());
                projectDto.setCustomer(CodeNameResponse.builder()
                        .id(cust.getId())
                        .code(cust.getCode())
                        .name(cust.getName())
                        .build());
                projectDto.setCustomerType(cust.getCustomerType());
                projectDto.setCustomerPhone(cust.getPhone());
                projectDto.setCustomerTaxCode(cust.getTaxCode());
                if (StringUtils.isBlank(projectDto.getOwner())) {
                    projectDto.setOwner(cust.getName());
                }
            });
        }

        Set<Long> staffIds = new HashSet<>();
        addIfNotNull(staffIds, projectEntity.getCreatorId());
        addIfNotNull(staffIds, projectEntity.getManagerId());
        addIfNotNull(staffIds, projectEntity.getSupporterId());
        Map<Long, CodeNameResponse> staffMap = staffRepository.getMapSimpleStaffByIds(staffIds);
        projectDto.setCreator(staffMap.get(projectEntity.getCreatorId()));
        projectDto.setManager(staffMap.get(projectEntity.getManagerId()));
        projectDto.setSupporter(staffMap.get(projectEntity.getSupporterId()));

        List<ProjectLineEntity> projectLineEntities = projectLineRepository.getListByProjectId(projectEntity.getId());
        List<ProjectCategoryMapEntity> projectCategoryMapEntities = projectCategoryMapRepository.getListByProjectId(projectEntity.getId());
        setProjectCategoryMaps(projectDto, projectCategoryMapEntities);
        setProjectCategoryLines(projectDto, projectLineEntities);
        checkProjectAccessScope(id);
        enrichProjectDetailMembersAndContracts(projectDto, id);
        if (projectEntity.getBuildingType() != null) {
            projectDto.setBuildingTypeLabel(projectEntity.getBuildingType().getLabel());
        }
        projectDto.setDocumentCount(projectDocumentRepository.getListByProjectId(id).size());
        return projectDto;
    }

    @Override
    public Page<ProjectOutput> getListProject(ProjectFilterParam param,
                                              Pageable pageable) {
        Collection<Long> scopedProjectIds = resolveScopedProjectIds();
        Page<ProjectEntity> projectEntities = projectRepository.getPageProject(param, pageable, scopedProjectIds);
        Set<Long> staffIds = new HashSet<>();
        Set<Long> customerIds = new HashSet<>();
        for (ProjectEntity projectEntity : projectEntities) {
            addIfNotNull(staffIds, projectEntity.getManagerId());
            addIfNotNull(customerIds, projectEntity.getCustomerId());
        }
        Map<Long, CodeNameResponse> staffMap = staffIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(staffIds);
        Map<Long, CustomerEntity> customerMap = customerIds.isEmpty()
                ? Collections.emptyMap()
                : customerRepository.getMapCustomerEntityByIds(customerIds);
        return projectEntities.map(entity -> buildProjectOutput(entity, staffMap, customerMap));
    }

    // ==================== 2.09: MỞ RỘNG VÒNG ĐỜI DỰ ÁN ====================

    @Override
    public ProjectLifecycleSummaryDto getLifecycleSummary() {
        Collection<Long> scopedProjectIds = resolveScopedProjectIds();
        List<ProjectEntity> allProjects = scopedProjectIds == null
                ? projectRepository.findAll()
                : (scopedProjectIds.isEmpty() ? Collections.emptyList() : projectRepository.findAllByIds(new ArrayList<>(scopedProjectIds)));
        Map<ProjectState, Long> countsByState = new EnumMap<>(ProjectState.class);
        Map<ProjectState, BigDecimal> valuesByState = new EnumMap<>(ProjectState.class);

        long activeCount = 0;
        long pausedCount = 0;
        long cancelledCount = 0;
        long closedCount = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (ProjectEntity p : allProjects) {
            ProjectState st = p.getState() != null ? p.getState() : ProjectState.RECEIVED;
            BigDecimal val = p.getContractValue() != null ? p.getContractValue() : BigDecimal.ZERO;

            countsByState.merge(st, 1L, Long::sum);
            valuesByState.merge(st, val, BigDecimal::add);
            if (st != ProjectState.CANCELLED) {
                grandTotal = grandTotal.add(val);
            }

            if (st == ProjectState.PAUSED) {
                pausedCount++;
            } else if (st == ProjectState.CANCELLED) {
                cancelledCount++;
            } else if (st == ProjectState.CLOSED) {
                closedCount++;
            } else if (st.isActiveWorkload()) {
                activeCount++;
            }
        }

        List<ProjectLifecycleSummaryDto.StageItem> stageItems = new ArrayList<>();
        for (ProjectState st : ProjectState.values()) {
            stageItems.add(ProjectLifecycleSummaryDto.StageItem.builder()
                    .state(st)
                    .label(st.getLabel())
                    .stepOrder(st.getStepOrder())
                    .projectCount(countsByState.getOrDefault(st, 0L))
                    .totalContractValue(valuesByState.getOrDefault(st, BigDecimal.ZERO))
                    .build());
        }

        return ProjectLifecycleSummaryDto.builder()
                .totalProjects(allProjects.size())
                .activeProjects(activeCount)
                .pausedProjects(pausedCount)
                .cancelledProjects(cancelledCount)
                .closedProjects(closedCount)
                .totalContractValue(grandTotal)
                .stages(stageItems)
                .build();
    }

    @Override
    @Transactional
    public IdResponse transitionState(Long projectId, ProjectStateTransitionDto input) {
        input.trimData();
        ProjectEntity project = projectRepository.getProjectById(projectId);
        ProjectState fromState = project.getState() != null ? project.getState() : ProjectState.RECEIVED;
        ProjectState toState = input.getToState();

        if (fromState == toState) {
            return IdResponse.builder().id(project.getId()).build();
        }

        validateTransitionRule(fromState, toState, project.getPreviousState());

        CustomerEntity linkedCustomer = project.getCustomerId() != null
                ? customerRepository.findById(project.getCustomerId()).orElse(null)
                : null;
        int categoryCount = projectCategoryMapRepository.getListByProjectId(projectId).size();

        validateStatePreConditions(project, linkedCustomer, fromState, toState, input.getReason(), categoryCount);
        applyStateTransitionMetadata(project, fromState, toState, input.getReason(),
                input.getActualHandoverDate(), input.getWarrantyEndDate());

        projectRepository.save(project);
        recordProjectStateLog(project, fromState, toState, input.getReason());
        syncCustomerPipelineOnProjectSave(linkedCustomer, toState);

        auditLogService.logExplicit(
                "CHANGE_PROJECT_STATE",
                "PROJECT",
                "ProjectEntity",
                String.valueOf(project.getId()),
                Map.of("state", fromState.name(), "label", fromState.getLabel()),
                Map.of(
                        "state", toState.name(),
                        "label", toState.getLabel(),
                        "reason", input.getReason() != null ? input.getReason() : ""
                ),
                "Chuyển vòng đời dự án " + project.getCode() + " (" + project.getName() + "): "
                        + fromState.getLabel() + " → " + toState.getLabel(),
                null
        );

        return IdResponse.builder().id(project.getId()).build();
    }

    @Override
    public List<ProjectStateLogDto> getStateLogs(Long projectId) {
        projectRepository.getProjectById(projectId);
        List<ProjectStateLogEntity> logs = projectStateLogRepository.getListByProjectId(projectId);
        Set<Long> staffIds = logs.stream()
                .map(ProjectStateLogEntity::getChangedByStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> staffMap = staffIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(staffIds);

        return logs.stream()
                .map(logEntity -> ProjectStateLogDto.builder()
                        .id(logEntity.getId())
                        .projectId(logEntity.getProjectId())
                        .fromState(logEntity.getFromState())
                        .fromStateLabel(logEntity.getFromState() != null ? logEntity.getFromState().getLabel() : "Khởi tạo")
                        .toState(logEntity.getToState())
                        .toStateLabel(logEntity.getToState() != null ? logEntity.getToState().getLabel() : "")
                        .reason(logEntity.getReason())
                        .changedByStaffId(logEntity.getChangedByStaffId())
                        .changedByStaff(logEntity.getChangedByStaffId() != null ? staffMap.get(logEntity.getChangedByStaffId()) : null)
                        .changedByUsername(logEntity.getChangedByUsername())
                        .createdAt(logEntity.getCreatedAt())
                        .build())
                .toList();
    }

    // ==================== BIỂU ĐỒ GANTT & TIẾN ĐỘ DỰ ÁN ====================

    @Override
    public ProjectGanttDto getProjectGantt(Long projectId) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        ProjectState currentState = project.getState() != null ? project.getState() : ProjectState.RECEIVED;
        LocalDate today = LocalDate.now();

        String customerName = project.getOwner();
        if (project.getCustomerId() != null) {
            CustomerEntity cust = customerRepository.findById(project.getCustomerId()).orElse(null);
            if (cust != null && StringUtils.isNotBlank(cust.getName())) {
                customerName = cust.getName();
            }
        }
        String managerName = null;
        if (project.getManagerId() != null) {
            Map<Long, CodeNameResponse> mgrMap = staffRepository.getMapSimpleStaffByIds(Set.of(project.getManagerId()));
            CodeNameResponse mgr = mgrMap.get(project.getManagerId());
            if (mgr != null) {
                managerName = mgr.getName();
            }
        }

        // Load phases (project_progress) – 2.21: sắp xếp theo sort_order
        List<ProjectProgressEntity> ppEntities = new ArrayList<>(projectProgressRepository.getListByProjectId(projectId));
        ppEntities.sort(Comparator
                .comparing((ProjectProgressEntity p) -> p.getSortOrder() != null ? p.getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(ProjectProgressEntity::getId));

        Set<Long> ppIds = ppEntities.stream().map(ProjectProgressEntity::getId).collect(Collectors.toSet());
        Set<Long> progressIds = ppEntities.stream()
                .map(ProjectProgressEntity::getProgressId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CodeNameResponse> progressCatalogMap = progressIds.isEmpty()
                ? Collections.emptyMap()
                : progressRepository.getMapProgressSimpleByIds(progressIds);

        List<ProjectProgressTaskMapEntity> ppTaskMaps = ppIds.isEmpty()
                ? Collections.emptyList()
                : projectProgressTaskMapRepository.getListByProjectProgressIds(ppIds);

        Set<Long> rootTaskIds = ppTaskMaps.stream()
                .map(ProjectProgressTaskMapEntity::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<TaskEntity> rootTasks = rootTaskIds.isEmpty()
                ? Collections.emptyList()
                : taskRepository.findAllByIds(rootTaskIds);
        List<TaskEntity> childTasks = rootTaskIds.isEmpty()
                ? Collections.emptyList()
                : taskRepository.getListTaskByParentIds(rootTaskIds);

        Map<Long, TaskEntity> rootTaskMap = rootTasks.stream()
                .collect(Collectors.toMap(TaskEntity::getId, t -> t, (a, b) -> a));
        Map<Long, List<TaskEntity>> childrenByParent = childTasks.stream()
                .filter(t -> t.getParentId() != null)
                .collect(Collectors.groupingBy(TaskEntity::getParentId));

        Set<Long> allTaskIds = new HashSet<>(rootTaskIds);
        childTasks.forEach(ct -> addIfNotNull(allTaskIds, ct.getId()));

        // Việc con cấp 3 trở xuống chỉ dùng để biết việc con có còn việc con hay không
        Set<Long> grandParentIds = childTasks.isEmpty()
                ? Collections.emptySet()
                : taskRepository.getListTaskByParentIds(childTasks.stream().map(TaskEntity::getId).collect(Collectors.toSet()))
                        .stream().map(TaskEntity::getParentId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<TaskStaffMapEntity> allTaskStaffMaps = allTaskIds.isEmpty()
                ? Collections.emptyList()
                : taskStaffMapRepository.getListByTaskIds(allTaskIds);

        Set<Long> allStaffIds = new HashSet<>();
        rootTasks.forEach(t -> addIfNotNull(allStaffIds, t.getReviewerId()));
        childTasks.forEach(t -> addIfNotNull(allStaffIds, t.getReviewerId()));
        allTaskStaffMaps.forEach(m -> addIfNotNull(allStaffIds, m.getStaffId()));

        Map<Long, CodeNameResponse> staffSimpleMap = allStaffIds.isEmpty()
                ? Collections.emptyMap()
                : staffRepository.getMapSimpleStaffByIds(allStaffIds);

        Map<Long, List<CodeNameResponse>> assigneesByTaskId = new HashMap<>();
        Map<Long, Set<Long>> assigneeIdsByTaskId = new HashMap<>();
        for (TaskStaffMapEntity m : allTaskStaffMaps) {
            if (m.getTaskId() == null || m.getStaffId() == null) continue;
            assigneeIdsByTaskId.computeIfAbsent(m.getTaskId(), k -> new HashSet<>()).add(m.getStaffId());
            CodeNameResponse s = staffSimpleMap.get(m.getStaffId());
            if (s != null) {
                assigneesByTaskId.computeIfAbsent(m.getTaskId(), k -> new ArrayList<>()).add(s);
            }
        }

        Map<Long, List<Long>> taskIdsByPpId = new HashMap<>();
        for (ProjectProgressTaskMapEntity m : ppTaskMaps) {
            if (m.getProjectProgressId() != null && m.getTaskId() != null) {
                taskIdsByPpId.computeIfAbsent(m.getProjectProgressId(), k -> new ArrayList<>()).add(m.getTaskId());
            }
        }

        // 2.23 / 2.25 / 2.27: ngữ cảnh workflow & số lượng bình luận / báo cáo hiện trường
        GanttContext ctx = new GanttContext(
                project.getManagerId(),
                taskWorkflowService.currentActor(),
                staffSimpleMap,
                assigneesByTaskId,
                assigneeIdsByTaskId,
                grandParentIds,
                taskCollaborationService.countCommentsByTaskIds(allTaskIds),
                taskCollaborationService.countSiteReportsByTaskIds(allTaskIds),
                today
        );

        List<LocalDate> candidateDates = new ArrayList<>();
        if (project.getSignDate() != null) candidateDates.add(project.getSignDate());
        if (project.getDeliveryDate() != null) candidateDates.add(project.getDeliveryDate());
        if (project.getActualHandoverDate() != null) candidateDates.add(project.getActualHandoverDate());

        GanttKpi kpi = new GanttKpi();

        List<ProjectGanttDto.PhaseItem> phaseItems = new ArrayList<>();
        for (ProjectProgressEntity pp : ppEntities) {
            CodeNameResponse progInfo = progressCatalogMap.get(pp.getProgressId());
            List<Long> mappedTaskIds = taskIdsByPpId.getOrDefault(pp.getId(), Collections.emptyList());

            List<ProjectGanttDto.TaskItem> phaseTaskDtos = new ArrayList<>();
            LocalDate phaseStart = null;
            LocalDate phaseEnd = null;
            int phaseLeafCompleted = 0;
            int phaseOverdue = 0;
            BigDecimal phaseWeighted = BigDecimal.ZERO;
            BigDecimal phaseWeightSum = BigDecimal.ZERO;

            List<TaskEntity> sortedRoots = mappedTaskIds.stream()
                    .map(rootTaskMap::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(
                            (TaskEntity t) -> t.getStartDate() != null ? t.getStartDate() : LocalDate.MAX
                    ).thenComparing(TaskEntity::getId))
                    .toList();

            for (TaskEntity rootTask : sortedRoots) {
                List<TaskEntity> subs = new ArrayList<>(childrenByParent.getOrDefault(rootTask.getId(), Collections.emptyList()));
                subs.sort(Comparator.comparing(
                        (TaskEntity t) -> t.getStartDate() != null ? t.getStartDate() : LocalDate.MAX
                ).thenComparing(TaskEntity::getId));

                List<ProjectGanttDto.TaskItem> subDtos = new ArrayList<>();
                for (TaskEntity sub : subs) {
                    ProjectGanttDto.TaskItem subItem = toGanttTaskItem(sub, pp.getId(), ctx, Collections.emptyList());
                    subDtos.add(subItem);
                    if (subItem.getStartDate() != null) candidateDates.add(subItem.getStartDate());
                    if (subItem.getEndDate() != null) candidateDates.add(subItem.getEndDate());
                    if (subItem.getActualEndDate() != null) candidateDates.add(subItem.getActualEndDate());
                    kpi.add(subItem);
                    if (subItem.isOverdue()) phaseOverdue++;
                }

                ProjectGanttDto.TaskItem rootItem = toGanttTaskItem(rootTask, pp.getId(), ctx, subDtos);

                // If root task has no explicit dates, derive from subTasks
                if (rootItem.getStartDate() == null && !subDtos.isEmpty()) {
                    rootItem.setStartDate(subDtos.stream()
                            .map(ProjectGanttDto.TaskItem::getStartDate)
                            .filter(Objects::nonNull)
                            .min(LocalDate::compareTo)
                            .orElse(null));
                }
                if (rootItem.getEndDate() == null && !subDtos.isEmpty()) {
                    rootItem.setEndDate(subDtos.stream()
                            .map(ProjectGanttDto.TaskItem::getEndDate)
                            .filter(Objects::nonNull)
                            .max(LocalDate::compareTo)
                            .orElse(null));
                }

                if (rootItem.getStartDate() != null) {
                    candidateDates.add(rootItem.getStartDate());
                    if (phaseStart == null || rootItem.getStartDate().isBefore(phaseStart)) {
                        phaseStart = rootItem.getStartDate();
                    }
                }
                if (rootItem.getEndDate() != null) {
                    candidateDates.add(rootItem.getEndDate());
                    if (phaseEnd == null || rootItem.getEndDate().isAfter(phaseEnd)) {
                        phaseEnd = rootItem.getEndDate();
                    }
                }
                if (rootItem.getActualEndDate() != null) candidateDates.add(rootItem.getActualEndDate());

                // Count root task in KPIs if it has no subTasks (leaf work item)
                if (subDtos.isEmpty()) {
                    kpi.add(rootItem);
                    if (rootItem.isOverdue()) phaseOverdue++;
                }

                if (rootItem.getState() == TaskState.COMPLETED) {
                    phaseLeafCompleted++;
                }
                BigDecimal w = ProgressCalculationService.weightOf(rootItem.getWeight());
                phaseWeighted = phaseWeighted.add((rootItem.getProgressPercent() != null ? rootItem.getProgressPercent() : BigDecimal.ZERO).multiply(w));
                phaseWeightSum = phaseWeightSum.add(w);

                phaseTaskDtos.add(rootItem);
            }

            BigDecimal phasePct = phaseWeightSum.compareTo(BigDecimal.ZERO) > 0
                    ? phaseWeighted.divide(phaseWeightSum, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            LocalDate plannedStart = pp.getPlannedStartDate() != null ? pp.getPlannedStartDate() : phaseStart;
            LocalDate plannedEnd = pp.getPlannedEndDate() != null ? pp.getPlannedEndDate() : phaseEnd;
            if (pp.getPlannedStartDate() != null) candidateDates.add(pp.getPlannedStartDate());
            if (pp.getPlannedEndDate() != null) candidateDates.add(pp.getPlannedEndDate());
            boolean phaseDone = !phaseTaskDtos.isEmpty() && phaseLeafCompleted == phaseTaskDtos.size();

            phaseItems.add(ProjectGanttDto.PhaseItem.builder()
                    .projectProgressId(pp.getId())
                    .progressId(pp.getProgressId())
                    .progressCode(progInfo != null ? progInfo.getCode() : null)
                    .progressName(progInfo != null ? progInfo.getName() : "Giai đoạn thi công")
                    .startDate(plannedStart)
                    .endDate(plannedEnd)
                    .progressPercent(phasePct)
                    .taskCount(phaseTaskDtos.size())
                    .completedTaskCount(phaseLeafCompleted)
                    .sortOrder(pp.getSortOrder())
                    .plannedStartDate(pp.getPlannedStartDate())
                    .plannedEndDate(pp.getPlannedEndDate())
                    .actualStartDate(pp.getActualStartDate())
                    .actualEndDate(pp.getActualEndDate())
                    .weight(ProgressCalculationService.weightOf(pp.getWeight()))
                    .note(pp.getNote())
                    .overdueTaskCount(phaseOverdue)
                    .delayDays(ScheduleStatusUtil.delayDays(plannedEnd, pp.getActualEndDate(), phaseDone, today))
                    .scheduleStatus(ScheduleStatusUtil.compute(plannedEnd, pp.getActualEndDate(), phaseDone, phasePct, today))
                    .tasks(phaseTaskDtos)
                    .build());
        }

        // Build Milestones (Contract + Payment Lines + Lifecycle Logs)
        List<ProjectGanttDto.MilestoneItem> milestones = new ArrayList<>();
        if (project.getSignDate() != null) {
            milestones.add(ProjectGanttDto.MilestoneItem.builder()
                    .key("contract-sign")
                    .type("CONTRACT")
                    .title("Ký kết hợp đồng")
                    .subtitle("Giá trị HĐ: " + (project.getContractValue() != null ? project.getContractValue().toPlainString() : "0"))
                    .date(project.getSignDate())
                    .amount(project.getContractValue())
                    .status("DONE")
                    .build());
        }
        if (project.getDeliveryDate() != null) {
            String delivStatus = project.getActualHandoverDate() != null
                    || currentState == ProjectState.HANDOVER
                    || currentState == ProjectState.WARRANTY
                    || currentState == ProjectState.CLOSED
                    ? "DONE"
                    : (project.getDeliveryDate().isBefore(today) ? "OVERDUE" : "UPCOMING");
            milestones.add(ProjectGanttDto.MilestoneItem.builder()
                    .key("contract-delivery")
                    .type("CONTRACT")
                    .title("Bàn giao dự kiến")
                    .subtitle("Mốc cam kết theo hợp đồng")
                    .date(project.getDeliveryDate())
                    .status(delivStatus)
                    .build());
        }
        if (project.getActualHandoverDate() != null) {
            milestones.add(ProjectGanttDto.MilestoneItem.builder()
                    .key("actual-handover")
                    .type("CONTRACT")
                    .title("Bàn giao thực tế")
                    .subtitle("Đã nghiệm thu & bàn giao công trình")
                    .date(project.getActualHandoverDate())
                    .status("DONE")
                    .build());
        }
        if (project.getWarrantyEndDate() != null) {
            milestones.add(ProjectGanttDto.MilestoneItem.builder()
                    .key("warranty-end")
                    .type("CONTRACT")
                    .title("Kết thúc bảo hành")
                    .subtitle("Hạn bảo hành công trình")
                    .date(project.getWarrantyEndDate())
                    .status(project.getWarrantyEndDate().isBefore(today) ? "DONE" : "INFO")
                    .build());
        }

        List<ProjectLineEntity> paymentLines = projectLineRepository.getListByProjectId(projectId);
        int idx = 1;
        for (ProjectLineEntity line : paymentLines) {
            if (line.getPaymentDate() == null) continue;
            candidateDates.add(line.getPaymentDate());
            long payNo = line.getPaymentNo() != null ? line.getPaymentNo() : idx;
            milestones.add(ProjectGanttDto.MilestoneItem.builder()
                    .key("payment-" + (line.getId() != null ? line.getId() : idx))
                    .type("PAYMENT")
                    .title("Thanh toán đợt " + payNo)
                    .subtitle("Lộ trình thu tiền hợp đồng")
                    .date(line.getPaymentDate())
                    .amount(line.getPaymentAmount())
                    .status(line.getPaymentDate().isAfter(today) ? "UPCOMING" : "DONE")
                    .build());
            idx++;
        }

        List<ProjectStateLogEntity> stateLogs = projectStateLogRepository.getListByProjectId(projectId);
        List<ProjectStateLogEntity> chronologicalLogs = new ArrayList<>(stateLogs);
        Collections.reverse(chronologicalLogs);
        for (ProjectStateLogEntity logEntity : chronologicalLogs) {
            if (logEntity.getCreatedAt() == null || logEntity.getToState() == null) continue;
            LocalDate logDate = logEntity.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            candidateDates.add(logDate);
            milestones.add(ProjectGanttDto.MilestoneItem.builder()
                    .key("lifecycle-" + logEntity.getId())
                    .type("LIFECYCLE")
                    .title("Mốc vòng đời: " + logEntity.getToState().getLabel())
                    .subtitle(logEntity.getReason())
                    .date(logDate)
                    .status("INFO")
                    .build());
        }

        milestones.sort(Comparator.comparing(
                (ProjectGanttDto.MilestoneItem m) -> m.getDate() != null ? m.getDate() : LocalDate.MAX
        ));

        LocalDate minDate = candidateDates.stream()
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(today.minusDays(7));
        LocalDate maxDate = candidateDates.stream()
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(today.plusDays(30));

        if (maxDate.isBefore(minDate.plusDays(14))) {
            maxDate = minDate.plusDays(21);
        }

        boolean projectDone = kpi.total > 0 && kpi.completed == kpi.total;
        BigDecimal projectPct = project.getProgressPercent() != null ? project.getProgressPercent() : BigDecimal.ZERO;
        LocalDate projectPlannedEnd = project.getDeliveryDate() != null
                ? project.getDeliveryDate()
                : phaseItems.stream().map(ProjectGanttDto.PhaseItem::getEndDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        LocalDate projectActualEnd = project.getActualHandoverDate();
        String projectSchedule = ScheduleStatusUtil.compute(projectPlannedEnd, projectActualEnd, projectDone, projectPct, today);
        if (kpi.overdue > 0 && ScheduleStatusUtil.ON_TRACK.equals(projectSchedule)) {
            projectSchedule = ScheduleStatusUtil.AT_RISK;
        }

        return ProjectGanttDto.builder()
                .projectId(project.getId())
                .projectCode(project.getCode())
                .projectName(project.getName())
                .customerName(customerName)
                .managerName(managerName)
                .state(currentState)
                .stateLabel(currentState.getLabel())
                .progressPercent(projectPct)
                .signDate(project.getSignDate())
                .deliveryDate(project.getDeliveryDate())
                .actualHandoverDate(project.getActualHandoverDate())
                .warrantyEndDate(project.getWarrantyEndDate())
                .timelineStartDate(minDate.minusDays(3))
                .timelineEndDate(maxDate.plusDays(5))
                .totalPhases(phaseItems.size())
                .totalTasks(kpi.total)
                .completedTasks(kpi.completed)
                .inProgressTasks(kpi.inProgress)
                .notStartedTasks(kpi.notStarted)
                .overdueTasks(kpi.overdue)
                .pendingReviewTasks(kpi.pendingReview)
                .returnedTasks(kpi.returned)
                .atRiskTasks(kpi.atRisk)
                .scheduleStatus(projectSchedule)
                .delayDays(ScheduleStatusUtil.delayDays(projectPlannedEnd, projectActualEnd, projectDone, today))
                .phases(phaseItems)
                .milestones(milestones)
                .build();
    }

    @Override
    @Transactional
    public IdResponse addGanttPhase(Long projectId, ProjectGanttPhaseRequestDto input) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        if (input != null) {
            input.trimData();
        }
        if (input != null && input.getPlannedStartDate() != null && input.getPlannedEndDate() != null
                && input.getPlannedEndDate().isBefore(input.getPlannedStartDate())) {
            throw BusinessException.exception("CST063");
        }
        Long targetProgressId = input != null ? input.getProgressId() : null;
        if (targetProgressId == null) {
            if (input == null || StringUtils.isBlank(input.getNewProgressName())) {
                throw BusinessException.exception("ERROR_0001");
            }
            ProgressEntity newProgress = ProgressEntity.builder()
                    .name(input.getNewProgressName())
                    .description(input.getDescription())
                    .isActive(true)
                    .build();
            generateCodeService.generateCode(newProgress, Constants.TT, ProgressEntity.class);
            uniqueValidationService.validate(newProgress);
            progressRepository.save(newProgress);
            targetProgressId = newProgress.getId();
        } else {
            progressRepository.getProgressById(targetProgressId);
        }

        int nextOrder = projectProgressRepository.getListByProjectId(projectId).stream()
                .map(ProjectProgressEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        ProjectProgressEntity ppEntity = ProjectProgressEntity.builder()
                .projectId(project.getId())
                .progressId(targetProgressId)
                .sortOrder(nextOrder)
                .plannedStartDate(input.getPlannedStartDate())
                .plannedEndDate(input.getPlannedEndDate())
                .weight(ProgressCalculationService.weightOf(input.getWeight()))
                .progressPercent(BigDecimal.ZERO)
                .note(input.getDescription())
                .build();
        projectProgressRepository.save(ppEntity);
        return IdResponse.builder().id(ppEntity.getId()).build();
    }

    @Override
    @Transactional
    public void deleteGanttPhase(Long projectId, Long projectProgressId) {
        projectRepository.getProjectById(projectId);
        ProjectProgressEntity ppEntity = projectProgressRepository.findById(projectProgressId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(ppEntity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST002");
        }
        // 2.21: xóa giai đoạn kèm cây công việc, đánh lại thứ tự & tính lại tiến độ
        projectProgressService.deletePhase(projectProgressId);
    }

    @Override
    @Transactional
    public IdResponse addGanttTask(Long projectId, ProjectGanttTaskRequestDto input) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        input.trimData();
        if (input.getEndDate().isBefore(input.getStartDate())) {
            throw BusinessException.exception("ERROR_0001");
        }

        // 2.23: Công việc mới chỉ có thể ở trạng thái Chưa làm / Đang làm (Hoàn thành phải qua duyệt)
        TaskState initialState = input.getState() == TaskState.IN_PROGRESS ? TaskState.IN_PROGRESS : TaskState.NOT_STARTED;
        // 2.22: % tiến độ do hệ thống tự tính từ trạng thái, không nhận giá trị nhập tay
        BigDecimal pct = BigDecimal.valueOf(initialState.getDefaultPercent());

        Long resolvedReviewerId = input.getReviewerId();
        if (resolvedReviewerId == null) {
            resolvedReviewerId = SecurityUtils.getCurrentUserLogin()
                    .flatMap(staffRepository::findOptionalStaffByUsername)
                    .map(StaffEntity::getId)
                    .orElse(project.getManagerId());
        }

        TaskEntity taskEntity = TaskEntity.builder()
                .name(input.getName())
                .description(input.getDescription())
                .startDate(input.getStartDate())
                .endDate(input.getEndDate())
                .priorityLevel(input.getPriorityLevel() != null ? input.getPriorityLevel() : PriorityLevel.MEDIUM)
                .state(initialState)
                .progressPercent(pct)
                .weight(ProgressCalculationService.weightOf(input.getWeight()))
                .actualStartDate(initialState == TaskState.IN_PROGRESS ? LocalDate.now() : null)
                .reviewerId(resolvedReviewerId)
                .parentId(input.getParentTaskId())
                .build();
        generateCodeService.generateCode(taskEntity, Constants.CV, TaskEntity.class);
        uniqueValidationService.validate(taskEntity);
        taskRepository.save(taskEntity);

        if (input.getStaffIds() != null && !input.getStaffIds().isEmpty()) {
            List<TaskStaffMapEntity> staffMaps = input.getStaffIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(sid -> TaskStaffMapEntity.builder()
                            .taskId(taskEntity.getId())
                            .staffId(sid)
                            .build())
                    .toList();
            if (!staffMaps.isEmpty()) {
                taskStaffMapRepository.saveAll(staffMaps);
            }
        }

        if (input.getParentTaskId() != null) {
            taskRepository.getTaskById(input.getParentTaskId());
        } else {
            Long ppId = input.getProjectProgressId();
            if (ppId == null) {
                throw BusinessException.exception("ERROR_0001");
            }
            ProjectProgressEntity ppEntity = projectProgressRepository.findById(ppId)
                    .orElseThrow(() -> BusinessException.exception("CST002"));
            if (!Objects.equals(ppEntity.getProjectId(), projectId)) {
                throw BusinessException.exception("CST002");
            }
            ProjectProgressTaskMapEntity mapEntity = ProjectProgressTaskMapEntity.builder()
                    .projectProgressId(ppId)
                    .taskId(taskEntity.getId())
                    .build();
            projectProgressTaskMapRepository.save(mapEntity);
        }

        // 2.22: tổng hợp lại việc cha → giai đoạn → dự án
        progressCalculationService.recalculateProject(projectId);
        return IdResponse.builder().id(taskEntity.getId()).build();
    }

    /** Ngữ cảnh dựng cây Gantt (tính 1 lần cho toàn bộ dự án). */
    private record GanttContext(Long projectManagerId,
                                TaskWorkflowService.Actor actor,
                                Map<Long, CodeNameResponse> staffMap,
                                Map<Long, List<CodeNameResponse>> assigneesMap,
                                Map<Long, Set<Long>> assigneeIdsMap,
                                Set<Long> deeperParentIds,
                                Map<Long, Long> commentCounts,
                                Map<Long, Long> siteReportCounts,
                                LocalDate today) {
    }

    /** Bộ đếm KPI công việc lá trên Gantt. */
    private static final class GanttKpi {
        int total;
        int completed;
        int inProgress;
        int notStarted;
        int pendingReview;
        int returned;
        int overdue;
        int atRisk;

        void add(ProjectGanttDto.TaskItem item) {
            total++;
            switch (item.getState()) {
                case COMPLETED -> completed++;
                case IN_PROGRESS -> inProgress++;
                case PENDING_REVIEW -> pendingReview++;
                case RETURNED -> returned++;
                default -> notStarted++;
            }
            if (item.isOverdue()) overdue++;
            if (ScheduleStatusUtil.AT_RISK.equals(item.getScheduleStatus())) atRisk++;
        }
    }

    private ProjectGanttDto.TaskItem toGanttTaskItem(TaskEntity entity,
                                                     Long projectProgressId,
                                                     GanttContext ctx,
                                                     List<ProjectGanttDto.TaskItem> subTasks) {
        LocalDate today = ctx.today();
        TaskState st = entity.getState() != null ? entity.getState() : TaskState.NOT_STARTED;
        BigDecimal pct = entity.getProgressPercent();
        if (pct == null) {
            pct = BigDecimal.valueOf(st.getDefaultPercent());
        }
        long duration = 1L;
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            duration = Math.max(1L, ChronoUnit.DAYS.between(entity.getStartDate(), entity.getEndDate()) + 1L);
        }
        boolean completed = st == TaskState.COMPLETED;
        Long remain = null;
        boolean overdue = false;
        if (entity.getEndDate() != null) {
            remain = ChronoUnit.DAYS.between(today, entity.getEndDate());
            overdue = !completed && entity.getEndDate().isBefore(today);
        }
        boolean hasChildren = (subTasks != null && !subTasks.isEmpty()) || ctx.deeperParentIds().contains(entity.getId());
        Set<Long> assigneeIds = ctx.assigneeIdsMap().getOrDefault(entity.getId(), Collections.emptySet());

        return ProjectGanttDto.TaskItem.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .parentId(entity.getParentId())
                .projectProgressId(projectProgressId)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .durationDays(duration)
                .remainDays(remain)
                .progressPercent(pct)
                .state(st)
                .stateLabel(st.getLabel())
                .priorityLevel(entity.getPriorityLevel())
                .overdue(overdue)
                .reviewer(entity.getReviewerId() != null ? ctx.staffMap().get(entity.getReviewerId()) : null)
                .assignees(ctx.assigneesMap().getOrDefault(entity.getId(), Collections.emptyList()))
                .weight(ProgressCalculationService.weightOf(entity.getWeight()))
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .delayDays(ScheduleStatusUtil.delayDays(entity.getEndDate(), entity.getActualEndDate(), completed, today))
                .scheduleStatus(ScheduleStatusUtil.compute(entity.getEndDate(), entity.getActualEndDate(), completed, pct, today))
                .returnReason(entity.getReturnReason())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .hasChildren(hasChildren)
                .canReview(taskWorkflowService.isReviewer(entity, ctx.projectManagerId(), ctx.actor()))
                .allowedNextStates(taskWorkflowService.allowedNextStates(entity, hasChildren, assigneeIds,
                        ctx.projectManagerId(), ctx.actor()))
                .siteReportCount(ctx.siteReportCounts().getOrDefault(entity.getId(), 0L))
                .commentCount(ctx.commentCounts().getOrDefault(entity.getId(), 0L))
                .subTasks(subTasks)
                .build();
    }

    private void recalculateProjectProgressPercent(Long projectId) {
        // 2.22: dùng chung bộ tính tiến độ có trọng số
        progressCalculationService.recalculateProject(projectId);
    }

    // ==================== STATE MACHINE & HELPERS ====================

    public List<ProjectState> computeAllowedNextStates(ProjectState current, ProjectState previousBeforePause) {
        if (current == null) {
            return List.of(ProjectState.SURVEYING, ProjectState.QUOTED, ProjectState.CONTRACT_SIGNED, ProjectState.PAUSED, ProjectState.CANCELLED);
        }
        if (current.isTerminal()) {
            return Collections.emptyList();
        }
        if (current == ProjectState.PAUSED) {
            ProjectState resumeTarget = (previousBeforePause != null
                    && !previousBeforePause.isTerminal()
                    && previousBeforePause != ProjectState.PAUSED)
                    ? previousBeforePause
                    : ProjectState.RECEIVED;
            return List.of(resumeTarget, ProjectState.CANCELLED);
        }
        return switch (current) {
            case RECEIVED -> List.of(
                    ProjectState.SURVEYING,
                    ProjectState.QUOTED,
                    ProjectState.CONTRACT_SIGNED,
                    ProjectState.PAUSED,
                    ProjectState.CANCELLED
            );
            case SURVEYING -> List.of(
                    ProjectState.QUOTED,
                    ProjectState.CONTRACT_SIGNED,
                    ProjectState.RECEIVED,
                    ProjectState.PAUSED,
                    ProjectState.CANCELLED
            );
            case QUOTED -> List.of(
                    ProjectState.CONTRACT_SIGNED,
                    ProjectState.SURVEYING,
                    ProjectState.PAUSED,
                    ProjectState.CANCELLED
            );
            case CONTRACT_SIGNED -> List.of(
                    ProjectState.IN_CONSTRUCTION,
                    ProjectState.PAUSED,
                    ProjectState.CANCELLED
            );
            case IN_CONSTRUCTION -> List.of(
                    ProjectState.ACCEPTANCE,
                    ProjectState.PAUSED,
                    ProjectState.CANCELLED
            );
            case ACCEPTANCE -> List.of(
                    ProjectState.HANDOVER,
                    ProjectState.IN_CONSTRUCTION,
                    ProjectState.PAUSED,
                    ProjectState.CANCELLED
            );
            case HANDOVER -> List.of(
                    ProjectState.WARRANTY,
                    ProjectState.CLOSED
            );
            case WARRANTY -> List.of(
                    ProjectState.CLOSED,
                    ProjectState.IN_CONSTRUCTION
            );
            default -> Collections.emptyList();
        };
    }

    private void validateTransitionRule(ProjectState fromState,
                                        ProjectState toState,
                                        ProjectState previousBeforePause) {
        if (fromState != null && fromState.isTerminal()) {
            throw BusinessException.exception("CST040");
        }
        List<ProjectState> allowed = computeAllowedNextStates(fromState, previousBeforePause);
        if (!allowed.contains(toState)) {
            throw BusinessException.exception("CST041");
        }
    }

    private void validateStatePreConditions(ProjectEntity project,
                                            CustomerEntity customer,
                                            ProjectState fromState,
                                            ProjectState toState,
                                            String reason,
                                            int categoryCount) {
        boolean isPauseOrCancel = (toState == ProjectState.PAUSED || toState == ProjectState.CANCELLED);
        boolean isRollback = fromState != null
                && fromState.isMainFlow()
                && toState.isMainFlow()
                && toState.getStepOrder() < fromState.getStepOrder();

        if ((isPauseOrCancel || isRollback) && StringUtils.isBlank(reason)) {
            throw BusinessException.exception("CST047");
        }

        if (isPauseOrCancel) {
            return;
        }

        switch (toState) {
            case SURVEYING -> {
                boolean hasOwner = customer != null
                        || project.getCustomerId() != null
                        || StringUtils.isNotBlank(project.getOwner());
                if (!hasOwner || StringUtils.isBlank(project.getAddress())) {
                    throw BusinessException.exception("CST042");
                }
            }
            case QUOTED -> {
                if (project.getContractValue() == null
                        || project.getContractValue().compareTo(BigDecimal.ZERO) <= 0
                        || categoryCount <= 0) {
                    throw BusinessException.exception("CST043");
                }
            }
            case CONTRACT_SIGNED -> {
                validateSignedContractRequirements(project, customer);
            }
            case IN_CONSTRUCTION -> {
                validateSignedContractRequirements(project, customer);
                if (project.getManagerId() == null || StringUtils.isBlank(project.getAddress())) {
                    throw BusinessException.exception("CST045");
                }
            }
            case ACCEPTANCE -> {
                if (project.getId() != null) {
                    List<TaskEntity> projectTasks = taskRepository.getListTaskByProjectId(project.getId());
                    boolean hasUnfinishedTask = projectTasks.stream()
                            .anyMatch(t -> t.getState() != TaskState.COMPLETED);
                    if (!projectTasks.isEmpty() && hasUnfinishedTask) {
                        throw BusinessException.exception("CST046");
                    }
                }
            }
            default -> {
                // RECEIVED, HANDOVER, WARRANTY, CLOSED checked via transition flow
            }
        }
    }

    private void validateSignedContractRequirements(ProjectEntity project, CustomerEntity customer) {
        boolean hasCustomer = customer != null || project.getCustomerId() != null;
        if (!hasCustomer
                || project.getSignDate() == null
                || project.getContractValue() == null
                || project.getContractValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.exception("CST044");
        }
        if (customer != null && customer.getCustomerType() == CustomerType.BUSINESS) {
            if (StringUtils.isBlank(customer.getTaxCode()) || StringUtils.isBlank(customer.getRepresentativeName())) {
                throw BusinessException.exception("CST033");
            }
        }
    }

    private void applyStateTransitionMetadata(ProjectEntity project,
                                              ProjectState fromState,
                                              ProjectState toState,
                                              String reason,
                                              LocalDate handoverDateInput,
                                              LocalDate warrantyEndDateInput) {
        if (toState == ProjectState.PAUSED) {
            project.setPreviousState(fromState);
        } else if (fromState == ProjectState.PAUSED) {
            project.setPreviousState(null);
        }
        project.setState(toState);
        project.setStateReason(reason);
        project.setStateChangedAt(LocalDateTime.now());
        applyAutomaticMilestoneDates(project, toState, handoverDateInput, warrantyEndDateInput);
    }

    private void applyAutomaticMilestoneDates(ProjectEntity project,
                                              ProjectState targetState,
                                              LocalDate handoverDateInput,
                                              LocalDate warrantyEndDateInput) {
        if (handoverDateInput != null) {
            project.setActualHandoverDate(handoverDateInput);
        } else if (targetState == ProjectState.HANDOVER && project.getActualHandoverDate() == null) {
            project.setActualHandoverDate(LocalDate.now());
        }

        if (warrantyEndDateInput != null) {
            project.setWarrantyEndDate(warrantyEndDateInput);
        } else if (targetState == ProjectState.WARRANTY && project.getWarrantyEndDate() == null) {
            LocalDate baseDate = project.getActualHandoverDate() != null
                    ? project.getActualHandoverDate()
                    : LocalDate.now();
            project.setWarrantyEndDate(baseDate.plusMonths(12));
        }

        if (targetState == ProjectState.HANDOVER || targetState == ProjectState.WARRANTY || targetState == ProjectState.CLOSED) {
            if (project.getProgressPercent() == null || project.getProgressPercent().compareTo(BigDecimal.valueOf(100)) < 0) {
                project.setProgressPercent(BigDecimal.valueOf(100));
            }
        }
    }

    private void recordProjectStateLog(ProjectEntity project,
                                       ProjectState fromState,
                                       ProjectState toState,
                                       String reason) {
        String currentUsername = SecurityUtils.getCurrentUserLogin().orElse("SYSTEM");
        Long actorStaffId = SecurityUtils.getCurrentUserLogin()
                .flatMap(staffRepository::findOptionalStaffByUsername)
                .map(StaffEntity::getId)
                .orElse(project.getCreatorId());

        ProjectStateLogEntity logEntity = ProjectStateLogEntity.builder()
                .projectId(project.getId())
                .fromState(fromState)
                .toState(toState)
                .reason(StringUtils.defaultIfBlank(reason,
                        fromState == null
                                ? "Khởi tạo dự án ở bước " + toState.getLabel()
                                : "Chuyển trạng thái: " + fromState.getLabel() + " → " + toState.getLabel()))
                .changedByStaffId(actorStaffId)
                .changedByUsername(currentUsername)
                .build();
        projectStateLogRepository.save(logEntity);
    }

    private CustomerEntity resolveLinkedCustomer(ProjectDto input) {
        Long custId = null;
        if (input.getCustomer() != null && input.getCustomer().getId() != null) {
            custId = input.getCustomer().getId();
        } else if (input.getCustomerId() != null) {
            custId = input.getCustomerId();
        }
        if (custId != null) {
            return customerRepository.getCustomerById(custId);
        }
        if (StringUtils.isBlank(input.getOwner())) {
            throw BusinessException.exception("CST037");
        }
        return null;
    }

    private void applyLinkedCustomerToProject(ProjectEntity projectEntity,
                                              CustomerEntity linkedCustomer,
                                              String fallbackOwner) {
        if (linkedCustomer != null) {
            projectEntity.setCustomerId(linkedCustomer.getId());
            projectEntity.setOwner(linkedCustomer.getName());
            if (StringUtils.isBlank(projectEntity.getAddress()) && StringUtils.isNotBlank(linkedCustomer.getAddress())) {
                projectEntity.setAddress(linkedCustomer.getAddress());
            }
        } else {
            projectEntity.setCustomerId(null);
            projectEntity.setOwner(fallbackOwner);
        }
    }

    private void syncCustomerPipelineOnProjectSave(CustomerEntity customer, ProjectState projectState) {
        if (customer == null || projectState == null) {
            return;
        }
        if (projectState == ProjectState.CANCELLED || projectState == ProjectState.PAUSED) {
            return;
        }

        CustomerPipelineStage currentStage = customer.getPipelineStage() != null
                ? customer.getPipelineStage()
                : CustomerPipelineStage.LEAD;
        CustomerPipelineStage targetStage = switch (projectState) {
            case RECEIVED, SURVEYING -> currentStage;
            case QUOTED -> currentStage.getStepOrder() < CustomerPipelineStage.QUOTED.getStepOrder()
                    ? CustomerPipelineStage.QUOTED
                    : currentStage;
            case CONTRACT_SIGNED -> (currentStage.getStepOrder() < CustomerPipelineStage.CONTRACT_SIGNED.getStepOrder()
                    || currentStage == CustomerPipelineStage.LOST)
                    ? CustomerPipelineStage.CONTRACT_SIGNED
                    : currentStage;
            case IN_CONSTRUCTION, ACCEPTANCE -> (currentStage.getStepOrder() < CustomerPipelineStage.IN_CONSTRUCTION.getStepOrder()
                    || currentStage == CustomerPipelineStage.LOST)
                    ? CustomerPipelineStage.IN_CONSTRUCTION
                    : currentStage;
            case HANDOVER, WARRANTY, CLOSED -> (currentStage.getStepOrder() < CustomerPipelineStage.WARRANTY.getStepOrder()
                    || currentStage == CustomerPipelineStage.LOST)
                    ? CustomerPipelineStage.WARRANTY
                    : currentStage;
            default -> currentStage;
        };

        boolean changed = false;
        if (targetStage != currentStage) {
            customer.setPipelineStage(targetStage);
            changed = true;
        }
        if (customer.getContactStatus() == null || customer.getContactStatus() == ContactStatus.NOT_CONTACTED) {
            customer.setContactStatus(ContactStatus.RESPONDED);
            changed = true;
        }
        if (changed) {
            customerRepository.save(customer);
        }
    }

    private void saveProjectLines(ProjectEntity projectEntity,
                                  List<ProjectLineDto> projectLines) {
        List<ProjectLineEntity> existingLines = projectLineRepository.getListByProjectId(projectEntity.getId());
        Map<Long, ProjectLineEntity> existingMap = existingLines.stream()
                .collect(Collectors.toMap(ProjectLineEntity::getId, l -> l));

        if (projectLines == null || projectLines.isEmpty()) {
            if (!existingLines.isEmpty()) {
                projectLineRepository.deleteAllInBatch(existingLines);
            }
            return;
        }

        BigDecimal contractVal = projectEntity.getContractValue() != null
                ? projectEntity.getContractValue()
                : BigDecimal.ZERO;

        Set<Long> retainedIds = new HashSet<>();
        List<ProjectLineEntity> toSave = new ArrayList<>();
        long order = 1;
        for (ProjectLineDto dto : projectLines) {
            if (dto == null) continue;
            ProjectLineEntity entity = (dto.getId() != null && existingMap.containsKey(dto.getId()))
                    ? existingMap.get(dto.getId())
                    : new ProjectLineEntity();

            if (entity.getId() != null) {
                retainedIds.add(entity.getId());
            }

            BigDecimal amount = dto.getPaymentAmount();
            BigDecimal pct = dto.getPercentage();

            if (amount == null && pct != null && contractVal.compareTo(BigDecimal.ZERO) > 0) {
                amount = contractVal.multiply(pct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (pct == null && amount != null && contractVal.compareTo(BigDecimal.ZERO) > 0) {
                pct = amount.multiply(BigDecimal.valueOf(100))
                        .divide(contractVal, 2, RoundingMode.HALF_UP);
            }

            entity.setProjectId(projectEntity.getId());
            entity.setPaymentNo(dto.getPaymentNo() != null ? dto.getPaymentNo() : order);
            entity.setPaymentDate(dto.getPaymentDate());
            entity.setPaymentAmount(amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            entity.setPercentage(pct != null ? pct.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            entity.setConditionMilestone(StringUtils.trimToNull(dto.getConditionMilestone()));
            entity.setStatus(dto.getStatus() != null ? dto.getStatus() : PaymentPlanStatus.PLANNED);
            entity.setNote(StringUtils.trimToNull(dto.getNote()));

            toSave.add(entity);
            order++;
        }

        List<ProjectLineEntity> toDelete = existingLines.stream()
                .filter(l -> !retainedIds.contains(l.getId()))
                .toList();
        if (!toDelete.isEmpty()) {
            projectLineRepository.deleteAllInBatch(toDelete);
        }
        if (!toSave.isEmpty()) {
            projectLineRepository.saveAll(toSave);
        }
    }

    private void saveProjectCategoryMaps(ProjectEntity projectEntity,
                                         List<ProjectCategoryMapDto> projectCategoryMaps) {
        if (projectCategoryMaps == null || projectCategoryMaps.isEmpty()) {
            return;
        }
        Set<Long> catIds = projectCategoryMaps.stream()
                .map(m -> m.getCategory() != null ? m.getCategory().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CategoryEntity> catEntityMap = catIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.findAllByIds(new ArrayList<>(catIds)).stream()
                        .collect(Collectors.toMap(CategoryEntity::getId, c -> c));

        List<ProjectCategoryMapEntity> projectCategoryMapEntities = new ArrayList<>();
        for (ProjectCategoryMapDto projectCategoryMap : projectCategoryMaps) {
            CodeNameResponse category = projectCategoryMap.getCategory();
            if (category == null || category.getId() == null) {
                throw BusinessException.exception("CST015");
            }
            CategoryEntity catEntity = catEntityMap.get(category.getId());
            String unit = StringUtils.trimToNull(projectCategoryMap.getUnit());
            if (unit == null && catEntity != null) {
                unit = catEntity.getUnit();
            }
            BigDecimal qty = projectCategoryMap.getQuantity() != null
                    ? projectCategoryMap.getQuantity().setScale(3, RoundingMode.HALF_UP)
                    : BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
            BigDecimal unitPrice = projectCategoryMap.getUnitPrice() != null
                    ? projectCategoryMap.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                    : (catEntity != null && catEntity.getDefaultUnitPrice() != null
                            ? catEntity.getDefaultUnitPrice().setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal total = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            ProjectCategoryMapEntity projectCategoryMapEntity = ProjectCategoryMapEntity.builder()
                    .projectId(projectEntity.getId())
                    .categoryId(category.getId())
                    .unit(unit)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .totalAmount(total)
                    .note(StringUtils.trimToNull(projectCategoryMap.getNote()))
                    .build();
            projectCategoryMapEntities.add(projectCategoryMapEntity);
        }
        projectCategoryMapRepository.saveAll(projectCategoryMapEntities);
    }

    private void deleteAllRelations(ProjectEntity projectEntity) {
        List<ProjectLineEntity> projectLineEntities = projectLineRepository.getListByProjectId(projectEntity.getId());
        List<ProjectCategoryMapEntity> projectCategoryMapEntities = projectCategoryMapRepository.getListByProjectId(projectEntity.getId());
        if (!projectLineEntities.isEmpty()) {
            projectLineRepository.deleteAllInBatch(projectLineEntities);
        }
        if (!projectCategoryMapEntities.isEmpty()) {
            projectCategoryMapRepository.deleteAllInBatch(projectCategoryMapEntities);
        }
    }

    private void setProjectCategoryMaps(ProjectDto projectDto,
                                        List<ProjectCategoryMapEntity> projectCategoryMapEntities) {
        Set<Long> categoryIds = new HashSet<>();
        for (ProjectCategoryMapEntity projectCategoryMapEntity : projectCategoryMapEntities) {
            addIfNotNull(categoryIds, projectCategoryMapEntity.getCategoryId());
        }
        Map<Long, CodeNameResponse> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.getMapSimpleCategoryByIds(categoryIds);
        List<ProjectCategoryMapDto> projectCategoryMaps = new ArrayList<>();
        for (ProjectCategoryMapEntity projectCategoryMapEntity : projectCategoryMapEntities) {
            ProjectCategoryMapDto projectCategoryMapDto = ProjectCategoryMapDto.builder()
                    .id(projectCategoryMapEntity.getId())
                    .category(getData(categoryMap, projectCategoryMapEntity.getCategoryId()))
                    .unit(projectCategoryMapEntity.getUnit())
                    .quantity(projectCategoryMapEntity.getQuantity())
                    .unitPrice(projectCategoryMapEntity.getUnitPrice())
                    .totalAmount(projectCategoryMapEntity.getTotalAmount())
                    .note(projectCategoryMapEntity.getNote())
                    .build();
            projectCategoryMaps.add(projectCategoryMapDto);
        }
        projectDto.setProjectCategoryMaps(projectCategoryMaps);
    }

    private void setProjectCategoryLines(ProjectDto projectDto,
                                         List<ProjectLineEntity> projectLineEntities) {
        List<ProjectPaymentEntity> payments = projectDto.getId() != null
                ? projectPaymentRepository.getListByProjectId(projectDto.getId())
                : Collections.emptyList();
        Map<Long, BigDecimal> collectedByLine = new HashMap<>();
        for (ProjectPaymentEntity p : payments) {
            if (p.getStatus() == ProjectPaymentStatus.CONFIRMED && p.getProjectLineId() != null && p.getAmount() != null) {
                collectedByLine.merge(p.getProjectLineId(), p.getAmount(), BigDecimal::add);
            }
        }
        LocalDate today = LocalDate.now();
        List<ProjectLineDto> projectLines = projectLineEntities.stream()
                .sorted(Comparator.comparing(ProjectLineEntity::getPaymentNo, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(ProjectLineEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(entity -> {
                    ProjectLineDto dto = projectLineMapper.toDto(entity);
                    BigDecimal planned = entity.getPaymentAmount() != null ? entity.getPaymentAmount() : BigDecimal.ZERO;
                    BigDecimal collected = collectedByLine.getOrDefault(entity.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal remaining = planned.subtract(collected).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

                    PaymentPlanStatus computedStatus = entity.getStatus() != null ? entity.getStatus() : PaymentPlanStatus.PLANNED;
                    if (planned.compareTo(BigDecimal.ZERO) > 0 && collected.compareTo(planned) >= 0) {
                        computedStatus = PaymentPlanStatus.PAID;
                    } else if (collected.compareTo(BigDecimal.ZERO) > 0) {
                        computedStatus = PaymentPlanStatus.PARTIAL;
                    } else if (entity.getPaymentDate() != null && entity.getPaymentDate().isBefore(today)) {
                        computedStatus = PaymentPlanStatus.OVERDUE;
                    } else if (entity.getPaymentDate() != null && !entity.getPaymentDate().isAfter(today.plusDays(7))) {
                        computedStatus = PaymentPlanStatus.DUE;
                    }

                    dto.setCollectedAmount(collected);
                    dto.setRemainingLineAmount(remaining);
                    dto.setStatus(computedStatus);
                    dto.setStatusLabel(computedStatus.getLabel());
                    return dto;
                })
                .toList();
        projectDto.setProjectLines(projectLines);
    }

    private ProjectOutput buildProjectOutput(ProjectEntity entity,
                                             Map<Long, CodeNameResponse> staffMap,
                                             Map<Long, CustomerEntity> customerMap) {
        ProjectOutput projectOutput = projectMapper.toOutput(entity);
        ProjectState currentState = entity.getState() != null ? entity.getState() : ProjectState.RECEIVED;
        projectOutput.setState(currentState);
        projectOutput.setAllowedNextStates(computeAllowedNextStates(currentState, entity.getPreviousState()));
        projectOutput.setManager(getData(staffMap, entity.getManagerId()));
        if (entity.getCustomerId() != null) {
            CustomerEntity cust = customerMap.get(entity.getCustomerId());
            if (cust != null) {
                projectOutput.setCustomerId(cust.getId());
                projectOutput.setCustomer(CodeNameResponse.builder()
                        .id(cust.getId())
                        .code(cust.getCode())
                        .name(cust.getName())
                        .build());
                projectOutput.setCustomerType(cust.getCustomerType());
                if (StringUtils.isBlank(projectOutput.getOwner())) {
                    projectOutput.setOwner(cust.getName());
                }
            }
        }
        projectOutput.setPaymentStatus(getPaymentStatus(entity.getRemainingAmount()));
        projectOutput.setMemberCount(projectMemberRepository.getListByProjectId(entity.getId()).size());
        projectOutput.setContractCount(contractRepository.getListByProjectId(entity.getId()).size());
        return projectOutput;
    }

    private PaymentStatus getPaymentStatus(BigDecimal remainingAmount) {
        if (remainingAmount != null && remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.NOT_COMPLETED;
        }
        return PaymentStatus.COMPLETED;
    }

    private BigDecimal calculateRemainingAmount(Long projectId,
                                                BigDecimal contractValue,
                                                BigDecimal contractAdvance) {
        BigDecimal value = contractValue != null ? contractValue : BigDecimal.ZERO;
        BigDecimal advance = contractAdvance != null ? contractAdvance : BigDecimal.ZERO;
        BigDecimal confirmedPayments = projectId != null
                ? projectPaymentRepository.sumConfirmedAmountByProjectId(projectId)
                : BigDecimal.ZERO;
        if (confirmedPayments == null) {
            confirmedPayments = BigDecimal.ZERO;
        }
        return value.subtract(advance.add(confirmedPayments))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void recalculateAndSaveProjectRemaining(ProjectEntity project) {
        project.setRemainingAmount(calculateRemainingAmount(
                project.getId(),
                project.getContractValue(),
                project.getContractAdvance()
        ));
        projectRepository.save(project);
    }

    // =========================================================================
    // 2.10: THÀNH VIÊN DỰ ÁN & PHÂN QUYỀN DỮ LIỆU
    // =========================================================================

    private Collection<Long> resolveScopedProjectIds() {
        if (isAdminUser()) {
            return null; // Admin: toàn quyền xem tất cả dự án
        }
        Long currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            return null;
        }
        List<ProjectEntity> participatingProjects = projectRepository.getListByParticipantStaffId(currentStaffId);
        return participatingProjects.stream().map(ProjectEntity::getId).collect(Collectors.toSet());
    }

    private void checkProjectAccessScope(Long projectId) {
        if (projectId == null || isAdminUser()) {
            return;
        }
        Long currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            return;
        }
        boolean isParticipant = projectMemberRepository.findByProjectIdAndStaffId(projectId, currentStaffId).isPresent();
        if (!isParticipant) {
            ProjectEntity p = projectRepository.getProjectById(projectId);
            isParticipant = Objects.equals(p.getCreatorId(), currentStaffId)
                    || Objects.equals(p.getManagerId(), currentStaffId)
                    || Objects.equals(p.getSupporterId(), currentStaffId);
        }
        if (!isParticipant) {
            throw BusinessException.exception("CST050");
        }
    }

    private boolean isAdminUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> {
            String val = a.getAuthority();
            return "ROLE_ADMIN".equalsIgnoreCase(val)
                    || "ADMIN".equalsIgnoreCase(val)
                    || "SCOPE_ADMIN".equalsIgnoreCase(val);
        });
    }

    private Long getCurrentStaffId() {
        Optional<String> loginOpt = SecurityUtils.getCurrentUserLogin();
        if (loginOpt.isEmpty() || StringUtils.isBlank(loginOpt.get())) {
            return null;
        }
        Optional<AccountEntity> accountOpt = accountRepository.findByUsername(loginOpt.get());
        if (accountOpt.isEmpty()) {
            return null;
        }
        Optional<AccountStaffMapEntity> mapOpt = accountStaffMapRepository.findOptionalByAccountId(accountOpt.get().getId());
        return mapOpt.map(AccountStaffMapEntity::getStaffId).orElse(null);
    }

    private void enrichProjectDetailMembersAndContracts(ProjectDto projectDto, Long projectId) {
        List<ProjectMemberDto> members = getProjectMembers(projectId);
        projectDto.setMembers(members);
        List<ContractDto> contracts = getProjectContracts(projectId);
        projectDto.setContracts(contracts);
    }

    private void syncProjectMembersOnCreate(ProjectEntity projectEntity, List<ProjectMemberDto> members) {
        Long projectId = projectEntity.getId();
        if (members != null && !members.isEmpty()) {
            for (ProjectMemberDto m : members) {
                if (m.getStaffId() == null || m.getMemberRole() == null) continue;
                ProjectMemberEntity entity = ProjectMemberEntity.builder()
                        .projectId(projectId)
                        .staffId(m.getStaffId())
                        .memberRole(m.getMemberRole())
                        .isPrimary(Boolean.TRUE.equals(m.getIsPrimary()))
                        .joinedDate(m.getJoinedDate() != null ? m.getJoinedDate() : LocalDate.now())
                        .note(m.getNote())
                        .build();
                projectMemberRepository.save(entity);
            }
        }
        ensureKeyProjectMembers(projectEntity);
    }

    private void syncProjectMembersOnUpdate(ProjectEntity projectEntity, List<ProjectMemberDto> members) {
        Long projectId = projectEntity.getId();
        if (members != null) {
            for (ProjectMemberDto m : members) {
                if (m.getStaffId() == null || m.getMemberRole() == null) continue;
                Optional<ProjectMemberEntity> existingOpt = projectMemberRepository.findByProjectIdAndStaffId(projectId, m.getStaffId());
                if (existingOpt.isPresent()) {
                    ProjectMemberEntity ex = existingOpt.get();
                    ex.setMemberRole(m.getMemberRole());
                    if (m.getIsPrimary() != null) ex.setIsPrimary(m.getIsPrimary());
                    if (m.getJoinedDate() != null) ex.setJoinedDate(m.getJoinedDate());
                    if (m.getNote() != null) ex.setNote(m.getNote());
                    projectMemberRepository.save(ex);
                } else {
                    ProjectMemberEntity entity = ProjectMemberEntity.builder()
                            .projectId(projectId)
                            .staffId(m.getStaffId())
                            .memberRole(m.getMemberRole())
                            .isPrimary(Boolean.TRUE.equals(m.getIsPrimary()))
                            .joinedDate(m.getJoinedDate() != null ? m.getJoinedDate() : LocalDate.now())
                            .note(m.getNote())
                            .build();
                    projectMemberRepository.save(entity);
                }
            }
        }
        ensureKeyProjectMembers(projectEntity);
    }

    private void ensureKeyProjectMembers(ProjectEntity projectEntity) {
        Long projectId = projectEntity.getId();
        if (projectEntity.getManagerId() != null) {
            if (projectMemberRepository.findByProjectIdAndStaffId(projectId, projectEntity.getManagerId()).isEmpty()) {
                projectMemberRepository.save(ProjectMemberEntity.builder()
                        .projectId(projectId)
                        .staffId(projectEntity.getManagerId())
                        .memberRole(ProjectMemberRole.PROJECT_MANAGER)
                        .isPrimary(true)
                        .joinedDate(LocalDate.now())
                        .build());
            }
        }
        if (projectEntity.getCreatorId() != null) {
            if (projectMemberRepository.findByProjectIdAndStaffId(projectId, projectEntity.getCreatorId()).isEmpty()) {
                projectMemberRepository.save(ProjectMemberEntity.builder()
                        .projectId(projectId)
                        .staffId(projectEntity.getCreatorId())
                        .memberRole(ProjectMemberRole.CREATOR)
                        .isPrimary(false)
                        .joinedDate(LocalDate.now())
                        .build());
            }
        }
        if (projectEntity.getSupporterId() != null) {
            if (projectMemberRepository.findByProjectIdAndStaffId(projectId, projectEntity.getSupporterId()).isEmpty()) {
                projectMemberRepository.save(ProjectMemberEntity.builder()
                        .projectId(projectId)
                        .staffId(projectEntity.getSupporterId())
                        .memberRole(ProjectMemberRole.SUPPORTER)
                        .isPrimary(false)
                        .joinedDate(LocalDate.now())
                        .build());
            }
        }
    }

    @Override
    public List<ProjectMemberDto> getProjectMembers(Long projectId) {
        List<ProjectMemberEntity> memberEntities = projectMemberRepository.getListByProjectId(projectId);
        if (memberEntities.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> staffIds = memberEntities.stream().map(ProjectMemberEntity::getStaffId).collect(Collectors.toSet());
        List<StaffEntity> staffEntities = staffRepository.findAllByIds(new ArrayList<>(staffIds));
        Map<Long, StaffEntity> staffMap = staffEntities.stream().collect(Collectors.toMap(StaffEntity::getId, s -> s));

        return memberEntities.stream().map(entity -> {
            ProjectMemberDto dto = projectMemberMapper.toDto(entity);
            dto.setMemberRoleLabel(entity.getMemberRole() != null ? entity.getMemberRole().getLabel() : "");
            StaffEntity staff = staffMap.get(entity.getStaffId());
            if (staff != null) {
                String fullName = (StringUtils.trimToEmpty(staff.getLastName()) + " " + StringUtils.trimToEmpty(staff.getFirstName())).trim();
                dto.setStaff(CodeNameResponse.builder()
                        .id(staff.getId())
                        .code(staff.getCode())
                        .name(fullName)
                        .build());
                dto.setStaffCode(staff.getCode());
                dto.setStaffName(fullName);
                dto.setStaffPhone(staff.getPhone());
                dto.setStaffEmail(staff.getEmail());
                dto.setStaffDepartment(staff.getDepartment() != null ? staff.getDepartment().getLabel() : "");
                dto.setStaffPosition(staff.getPosition() != null ? staff.getPosition().getLabel() : "");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IdResponse addProjectMember(Long projectId, ProjectMemberDto input) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        StaffEntity staff = staffRepository.findById(input.getStaffId())
                .orElseThrow(() -> BusinessException.exception("CST002"));

        Optional<ProjectMemberEntity> existing = projectMemberRepository.findByProjectIdAndStaffId(projectId, input.getStaffId());
        if (existing.isPresent() && existing.get().getMemberRole() == input.getMemberRole()) {
            throw BusinessException.exception("CST049");
        }

        ProjectMemberEntity entity = ProjectMemberEntity.builder()
                .projectId(projectId)
                .staffId(input.getStaffId())
                .memberRole(input.getMemberRole())
                .isPrimary(Boolean.TRUE.equals(input.getIsPrimary()))
                .joinedDate(input.getJoinedDate() != null ? input.getJoinedDate() : LocalDate.now())
                .note(StringUtils.trimToNull(input.getNote()))
                .build();
        projectMemberRepository.save(entity);

        if (entity.getMemberRole() == ProjectMemberRole.PROJECT_MANAGER && Boolean.TRUE.equals(entity.getIsPrimary())) {
            project.setManagerId(staff.getId());
            projectRepository.save(project);
        }

        auditLogService.logExplicit(
                "ADD_PROJECT_MEMBER",
                "PROJECT",
                "ProjectMemberEntity",
                String.valueOf(entity.getId()),
                null,
                Map.of("staffId", staff.getId(), "role", entity.getMemberRole().name()),
                "Thêm thành viên " + staff.getCode() + " vào dự án " + project.getCode() + " vai trò " + entity.getMemberRole().getLabel(),
                null
        );

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse updateProjectMember(Long projectId, Long memberId, ProjectMemberDto input) {
        ProjectMemberEntity entity = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.exception("CST048"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST048");
        }
        if (input.getMemberRole() != null) {
            entity.setMemberRole(input.getMemberRole());
        }
        if (input.getIsPrimary() != null) {
            entity.setIsPrimary(input.getIsPrimary());
        }
        if (input.getJoinedDate() != null) {
            entity.setJoinedDate(input.getJoinedDate());
        }
        if (input.getNote() != null) {
            entity.setNote(StringUtils.trimToNull(input.getNote()));
        }
        projectMemberRepository.save(entity);

        if (entity.getMemberRole() == ProjectMemberRole.PROJECT_MANAGER && Boolean.TRUE.equals(entity.getIsPrimary())) {
            ProjectEntity project = projectRepository.getProjectById(projectId);
            project.setManagerId(entity.getStaffId());
            projectRepository.save(project);
        }

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public void removeProjectMember(Long projectId, Long memberId) {
        ProjectMemberEntity entity = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.exception("CST048"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST048");
        }
        projectMemberRepository.delete(entity);
    }

    // =========================================================================
    // 2.11: HỢP ĐỒNG & PHỤ LỤC HỢP ĐỒNG
    // =========================================================================

    @Override
    public List<ContractDto> getProjectContracts(Long projectId) {
        List<ContractEntity> contractEntities = contractRepository.getListByProjectId(projectId);
        if (contractEntities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> contractIds = contractEntities.stream().map(ContractEntity::getId).toList();
        List<ContractAddendumEntity> addendums = contractAddendumRepository.getListByContractIds(contractIds);
        Map<Long, List<ContractAddendumEntity>> addendumMap = addendums.stream()
                .collect(Collectors.groupingBy(ContractAddendumEntity::getContractId));

        return contractEntities.stream().map(c -> {
            ContractDto dto = contractMapper.toDto(c);
            dto.setContractTypeLabel(c.getContractType() != null ? c.getContractType().getLabel() : "");
            dto.setStatusLabel(c.getStatus() != null ? c.getStatus().getLabel() : "");

            List<ContractAddendumEntity> cAddendums = addendumMap.getOrDefault(c.getId(), Collections.emptyList());
            List<ContractAddendumDto> addendumDtos = cAddendums.stream().map(a -> {
                ContractAddendumDto aDto = contractAddendumMapper.toDto(a);
                aDto.setAdjustmentTypeLabel(a.getAdjustmentType() != null ? a.getAdjustmentType().getLabel() : "");
                return aDto;
            }).collect(Collectors.toList());
            dto.setAddendums(addendumDtos);
            dto.setTotalAddendumCount(BigDecimal.valueOf(addendumDtos.size()));

            BigDecimal totalAdj = c.getValueAfterTax() != null ? c.getValueAfterTax() : BigDecimal.ZERO;
            for (ContractAddendumEntity a : cAddendums) {
                BigDecimal aVal = a.getValueAfterTax() != null ? a.getValueAfterTax() : BigDecimal.ZERO;
                if (a.getAdjustmentType() == AddendumAdjustmentType.INCREASE) {
                    totalAdj = totalAdj.add(aVal);
                } else if (a.getAdjustmentType() == AddendumAdjustmentType.DECREASE) {
                    totalAdj = totalAdj.subtract(aVal);
                }
            }
            dto.setTotalAdjustedValue(totalAdj.setScale(2, RoundingMode.HALF_UP));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public ContractDto getContractDetail(Long projectId, Long contractId) {
        ContractEntity c = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.exception("CST051"));
        if (!Objects.equals(c.getProjectId(), projectId)) {
            throw BusinessException.exception("CST051");
        }
        ContractDto dto = contractMapper.toDto(c);
        dto.setContractTypeLabel(c.getContractType() != null ? c.getContractType().getLabel() : "");
        dto.setStatusLabel(c.getStatus() != null ? c.getStatus().getLabel() : "");

        List<ContractAddendumEntity> cAddendums = contractAddendumRepository.getListByContractId(contractId);
        List<ContractAddendumDto> addendumDtos = cAddendums.stream().map(a -> {
            ContractAddendumDto aDto = contractAddendumMapper.toDto(a);
            aDto.setAdjustmentTypeLabel(a.getAdjustmentType() != null ? a.getAdjustmentType().getLabel() : "");
            return aDto;
        }).collect(Collectors.toList());
        dto.setAddendums(addendumDtos);
        dto.setTotalAddendumCount(BigDecimal.valueOf(addendumDtos.size()));

        BigDecimal totalAdj = c.getValueAfterTax() != null ? c.getValueAfterTax() : BigDecimal.ZERO;
        for (ContractAddendumEntity a : cAddendums) {
            BigDecimal aVal = a.getValueAfterTax() != null ? a.getValueAfterTax() : BigDecimal.ZERO;
            if (a.getAdjustmentType() == AddendumAdjustmentType.INCREASE) {
                totalAdj = totalAdj.add(aVal);
            } else if (a.getAdjustmentType() == AddendumAdjustmentType.DECREASE) {
                totalAdj = totalAdj.subtract(aVal);
            }
        }
        dto.setTotalAdjustedValue(totalAdj.setScale(2, RoundingMode.HALF_UP));
        return dto;
    }

    @Override
    @Transactional
    public IdResponse createContract(Long projectId, ContractDto input) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        String contractNum = StringUtils.trimToEmpty(input.getContractNumber());
        if (contractRepository.findByContractNumber(contractNum).isPresent()) {
            throw BusinessException.exception("CST052");
        }

        ContractEntity entity = contractMapper.toEntity(input);
        entity.setProjectId(projectId);
        calculateContractTaxValues(entity);

        contractRepository.save(entity);

        if (entity.getContractType() == ContractType.MAIN) {
            project.setContractValue(entity.getValueAfterTax());
            if (entity.getAdvanceAmount() != null) {
                project.setContractAdvance(entity.getAdvanceAmount());
            }
            project.setRemainingAmount(calculateRemainingAmount(projectId, entity.getValueAfterTax(), entity.getAdvanceAmount()));
            if (entity.getSignDate() != null) {
                project.setSignDate(entity.getSignDate());
            }
            if (entity.getEndDate() != null) {
                project.setDeliveryDate(entity.getEndDate());
            }
            projectRepository.save(project);
        }

        auditLogService.logExplicit(
                "CREATE_CONTRACT",
                "PROJECT",
                "ContractEntity",
                String.valueOf(entity.getId()),
                null,
                Map.of("contractNumber", entity.getContractNumber(), "valueAfterTax", entity.getValueAfterTax()),
                "Tạo hợp đồng " + entity.getContractNumber() + " cho dự án " + project.getCode(),
                null
        );

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse updateContract(Long projectId, Long contractId, ContractDto input) {
        ContractEntity entity = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.exception("CST051"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST051");
        }

        String newNum = StringUtils.trimToEmpty(input.getContractNumber());
        if (!newNum.equalsIgnoreCase(entity.getContractNumber())) {
            if (contractRepository.findByContractNumber(newNum).isPresent()) {
                throw BusinessException.exception("CST052");
            }
            entity.setContractNumber(newNum);
        }

        entity.setName(StringUtils.trimToEmpty(input.getName()));
        if (input.getContractType() != null) entity.setContractType(input.getContractType());
        if (input.getStatus() != null) entity.setStatus(input.getStatus());
        entity.setSignDate(input.getSignDate());
        entity.setStartDate(input.getStartDate());
        entity.setEndDate(input.getEndDate());
        entity.setValueBeforeTax(input.getValueBeforeTax());
        entity.setVatPercent(input.getVatPercent());
        entity.setAdvanceAmount(input.getAdvanceAmount());
        entity.setWarrantyMonths(input.getWarrantyMonths());
        entity.setWarrantyPercent(input.getWarrantyPercent());
        entity.setTerms(input.getTerms());
        entity.setNote(input.getNote());
        if (input.getContractFiles() != null) entity.setContractFiles(input.getContractFiles());

        calculateContractTaxValues(entity);
        contractRepository.save(entity);

        if (entity.getContractType() == ContractType.MAIN) {
            ProjectEntity project = projectRepository.getProjectById(projectId);
            project.setContractValue(entity.getValueAfterTax());
            if (entity.getAdvanceAmount() != null) {
                project.setContractAdvance(entity.getAdvanceAmount());
            }
            project.setRemainingAmount(calculateRemainingAmount(projectId, entity.getValueAfterTax(), entity.getAdvanceAmount()));
            if (entity.getSignDate() != null) {
                project.setSignDate(entity.getSignDate());
            }
            if (entity.getEndDate() != null) {
                project.setDeliveryDate(entity.getEndDate());
            }
            projectRepository.save(project);
        }

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public void deleteContract(Long projectId, Long contractId) {
        ContractEntity entity = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.exception("CST051"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST051");
        }
        contractAddendumRepository.deleteAllByContractId(contractId);
        contractRepository.delete(entity);
    }

    @Override
    @Transactional
    public IdResponse addContractAddendum(Long projectId, Long contractId, ContractAddendumDto input) {
        ContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> BusinessException.exception("CST051"));
        if (!Objects.equals(contract.getProjectId(), projectId)) {
            throw BusinessException.exception("CST051");
        }

        String addendumNum = StringUtils.trimToEmpty(input.getAddendumNumber());
        if (contractAddendumRepository.findByContractIdAndAddendumNumber(contractId, addendumNum).isPresent()) {
            throw BusinessException.exception("CST054");
        }

        ContractAddendumEntity entity = contractAddendumMapper.toEntity(input);
        entity.setContractId(contractId);
        entity.setProjectId(projectId);
        calculateAddendumTaxValues(entity);

        contractAddendumRepository.save(entity);

        if (entity.getNewEndDate() != null) {
            contract.setEndDate(entity.getNewEndDate());
            contractRepository.save(contract);
            ProjectEntity project = projectRepository.getProjectById(projectId);
            project.setDeliveryDate(entity.getNewEndDate());
            projectRepository.save(project);
        }

        auditLogService.logExplicit(
                "ADD_CONTRACT_ADDENDUM",
                "PROJECT",
                "ContractAddendumEntity",
                String.valueOf(entity.getId()),
                null,
                Map.of("addendumNumber", entity.getAddendumNumber(), "adjustmentType", entity.getAdjustmentType().name()),
                "Thêm phụ lục " + entity.getAddendumNumber() + " cho HĐ " + contract.getContractNumber(),
                null
        );

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse updateContractAddendum(Long projectId, Long contractId, Long addendumId, ContractAddendumDto input) {
        ContractAddendumEntity entity = contractAddendumRepository.findById(addendumId)
                .orElseThrow(() -> BusinessException.exception("CST053"));
        if (!Objects.equals(entity.getContractId(), contractId) || !Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST053");
        }

        String newNum = StringUtils.trimToEmpty(input.getAddendumNumber());
        if (!newNum.equalsIgnoreCase(entity.getAddendumNumber())) {
            if (contractAddendumRepository.findByContractIdAndAddendumNumber(contractId, newNum).isPresent()) {
                throw BusinessException.exception("CST054");
            }
            entity.setAddendumNumber(newNum);
        }

        entity.setName(StringUtils.trimToEmpty(input.getName()));
        if (input.getAdjustmentType() != null) entity.setAdjustmentType(input.getAdjustmentType());
        entity.setSignDate(input.getSignDate());
        entity.setEffectiveDate(input.getEffectiveDate());
        entity.setAdjustedDays(input.getAdjustedDays());
        entity.setNewEndDate(input.getNewEndDate());
        entity.setValueBeforeTax(input.getValueBeforeTax());
        entity.setVatPercent(input.getVatPercent());
        entity.setReason(input.getReason());
        entity.setContent(input.getContent());
        entity.setNote(input.getNote());
        if (input.getAddendumFiles() != null) entity.setAddendumFiles(input.getAddendumFiles());

        calculateAddendumTaxValues(entity);
        contractAddendumRepository.save(entity);

        return IdResponse.builder().id(entity.getId()).build();
    }

    @Override
    @Transactional
    public void deleteContractAddendum(Long projectId, Long contractId, Long addendumId) {
        ContractAddendumEntity entity = contractAddendumRepository.findById(addendumId)
                .orElseThrow(() -> BusinessException.exception("CST053"));
        if (!Objects.equals(entity.getContractId(), contractId) || !Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST053");
        }
        contractAddendumRepository.delete(entity);
    }

    private void calculateContractTaxValues(ContractEntity entity) {
        BigDecimal valBefore = entity.getValueBeforeTax() != null ? entity.getValueBeforeTax() : BigDecimal.ZERO;
        BigDecimal vatPct = entity.getVatPercent() != null ? entity.getVatPercent() : BigDecimal.valueOf(10.0);
        BigDecimal vatAmt = valBefore.multiply(vatPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal valAfter = valBefore.add(vatAmt).setScale(2, RoundingMode.HALF_UP);

        entity.setValueBeforeTax(valBefore.setScale(2, RoundingMode.HALF_UP));
        entity.setVatPercent(vatPct.setScale(2, RoundingMode.HALF_UP));
        entity.setVatAmount(vatAmt);
        entity.setValueAfterTax(valAfter);

        if (entity.getWarrantyPercent() != null && entity.getWarrantyPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal wAmt = valAfter.multiply(entity.getWarrantyPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            entity.setWarrantyAmount(wAmt);
        }
    }

    private void calculateAddendumTaxValues(ContractAddendumEntity entity) {
        BigDecimal valBefore = entity.getValueBeforeTax() != null ? entity.getValueBeforeTax() : BigDecimal.ZERO;
        BigDecimal vatPct = entity.getVatPercent() != null ? entity.getVatPercent() : BigDecimal.valueOf(10.0);
        BigDecimal vatAmt = valBefore.multiply(vatPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal valAfter = valBefore.add(vatAmt).setScale(2, RoundingMode.HALF_UP);

        entity.setValueBeforeTax(valBefore.setScale(2, RoundingMode.HALF_UP));
        entity.setVatPercent(vatPct.setScale(2, RoundingMode.HALF_UP));
        entity.setVatAmount(vatAmt);
        entity.setValueAfterTax(valAfter);
    }

    // =========================================================================
    // 2.13 & 2.14: KẾ HOẠCH ĐỢT THANH TOÁN & GHI NHẬN THU TIỀN THỰC TẾ
    // =========================================================================

    @Override
    @Transactional
    public List<ProjectLineDto> savePaymentPlans(Long projectId, List<ProjectLineDto> lines) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);
        saveProjectLines(project, lines);

        ProjectDto tempDto = ProjectDto.builder().id(projectId).build();
        setProjectCategoryLines(tempDto, projectLineRepository.getListByProjectId(projectId));
        return tempDto.getProjectLines();
    }

    @Override
    public ProjectPaymentSummaryDto getProjectPayments(Long projectId) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);

        List<ProjectLineEntity> planLines = projectLineRepository.getListByProjectId(projectId);
        Map<Long, ProjectLineEntity> lineMap = planLines.stream()
                .collect(Collectors.toMap(ProjectLineEntity::getId, l -> l));

        List<ProjectPaymentEntity> paymentEntities = projectPaymentRepository.getListByProjectId(projectId);
        BigDecimal confirmedTotal = BigDecimal.ZERO;
        BigDecimal pendingTotal = BigDecimal.ZERO;

        List<ProjectPaymentDto> paymentDtos = new ArrayList<>();
        for (ProjectPaymentEntity entity : paymentEntities) {
            if (entity.getStatus() == ProjectPaymentStatus.CONFIRMED && entity.getAmount() != null) {
                confirmedTotal = confirmedTotal.add(entity.getAmount());
            } else if (entity.getStatus() == ProjectPaymentStatus.PENDING && entity.getAmount() != null) {
                pendingTotal = pendingTotal.add(entity.getAmount());
            }
            paymentDtos.add(toProjectPaymentDto(entity, lineMap));
        }

        BigDecimal contractValue = project.getContractValue() != null
                ? project.getContractValue().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal contractAdvance = project.getContractAdvance() != null
                ? project.getContractAdvance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCollected = contractAdvance.add(confirmedTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = contractValue.subtract(totalCollected).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal collectionRate = contractValue.compareTo(BigDecimal.ZERO) > 0
                ? totalCollected.multiply(BigDecimal.valueOf(100))
                        .divide(contractValue, 2, RoundingMode.HALF_UP)
                        .min(BigDecimal.valueOf(100))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return ProjectPaymentSummaryDto.builder()
                .projectId(projectId)
                .contractValue(contractValue)
                .contractAdvance(contractAdvance)
                .confirmedPaymentTotal(confirmedTotal.setScale(2, RoundingMode.HALF_UP))
                .pendingPaymentTotal(pendingTotal.setScale(2, RoundingMode.HALF_UP))
                .totalCollected(totalCollected)
                .remainingAmount(remaining)
                .collectionRate(collectionRate)
                .payments(paymentDtos)
                .build();
    }

    @Override
    @Transactional
    public ProjectPaymentDto createProjectPayment(Long projectId, ProjectPaymentDto input) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);

        ProjectPaymentEntity entity = projectPaymentMapper.toEntity(input);
        entity.setId(null);
        entity.setProjectId(projectId);
        entity.setAmount(input.getAmount().setScale(2, RoundingMode.HALF_UP));
        entity.setPaymentMethod(input.getPaymentMethod() != null ? input.getPaymentMethod() : PaymentMethod.BANK_TRANSFER);
        entity.setStatus(input.getStatus() != null ? input.getStatus() : ProjectPaymentStatus.CONFIRMED);
        entity.setPayerName(StringUtils.trimToNull(input.getPayerName()));
        entity.setNote(StringUtils.trimToNull(input.getNote()));

        String code = StringUtils.trimToNull(input.getReceiptCode());
        if (code == null) {
            int nextIdx = projectPaymentRepository.getListByProjectId(projectId).size() + 1;
            code = String.format("PT-%s-%03d",
                    StringUtils.isNotBlank(project.getCode()) ? project.getCode() : String.valueOf(projectId),
                    nextIdx);
        }
        entity.setReceiptCode(code);

        if (entity.getProjectLineId() != null) {
            projectLineRepository.findById(entity.getProjectLineId()).ifPresent(line -> {
                if (!Objects.equals(line.getProjectId(), projectId)) {
                    throw BusinessException.exception("CST002");
                }
            });
        }

        projectPaymentRepository.save(entity);
        recalculateAndSaveProjectRemaining(project);

        auditLogService.logExplicit(
                "RECORD_PROJECT_PAYMENT",
                "PROJECT",
                "ProjectPaymentEntity",
                String.valueOf(entity.getId()),
                null,
                Map.of(
                        "receiptCode", entity.getReceiptCode(),
                        "amount", entity.getAmount(),
                        "status", entity.getStatus().name()
                ),
                "Ghi nhận phiếu thu " + entity.getReceiptCode() + " (" + entity.getAmount() + " VNĐ) cho dự án " + project.getCode(),
                null
        );

        Map<Long, ProjectLineEntity> lineMap = projectLineRepository.getListByProjectId(projectId).stream()
                .collect(Collectors.toMap(ProjectLineEntity::getId, l -> l));
        return toProjectPaymentDto(entity, lineMap);
    }

    @Override
    @Transactional
    public ProjectPaymentDto updateProjectPayment(Long projectId, Long paymentId, ProjectPaymentDto input) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);
        ProjectPaymentEntity entity = projectPaymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST002");
        }

        if (StringUtils.isNotBlank(input.getReceiptCode())) {
            entity.setReceiptCode(input.getReceiptCode().trim());
        }
        entity.setProjectLineId(input.getProjectLineId());
        entity.setPaymentDate(input.getPaymentDate());
        entity.setAmount(input.getAmount().setScale(2, RoundingMode.HALF_UP));
        if (input.getPaymentMethod() != null) {
            entity.setPaymentMethod(input.getPaymentMethod());
        }
        if (input.getStatus() != null) {
            entity.setStatus(input.getStatus());
        }
        entity.setPayerName(StringUtils.trimToNull(input.getPayerName()));
        entity.setNote(StringUtils.trimToNull(input.getNote()));
        if (input.getAttachments() != null) {
            entity.setAttachments(input.getAttachments());
        }

        projectPaymentRepository.save(entity);
        recalculateAndSaveProjectRemaining(project);

        Map<Long, ProjectLineEntity> lineMap = projectLineRepository.getListByProjectId(projectId).stream()
                .collect(Collectors.toMap(ProjectLineEntity::getId, l -> l));
        return toProjectPaymentDto(entity, lineMap);
    }

    @Override
    @Transactional
    public ProjectPaymentDto updateProjectPaymentStatus(Long projectId, Long paymentId, ProjectPaymentStatus status) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);
        ProjectPaymentEntity entity = projectPaymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST002");
        }

        ProjectPaymentStatus prev = entity.getStatus();
        entity.setStatus(status);
        projectPaymentRepository.save(entity);
        recalculateAndSaveProjectRemaining(project);

        auditLogService.logExplicit(
                "UPDATE_PROJECT_PAYMENT_STATUS",
                "PROJECT",
                "ProjectPaymentEntity",
                String.valueOf(entity.getId()),
                Map.of("status", prev != null ? prev.name() : ""),
                Map.of("status", status.name()),
                "Cập nhật trạng thái phiếu thu " + entity.getReceiptCode() + " từ " + (prev != null ? prev.getLabel() : "—") + " sang " + status.getLabel(),
                null
        );

        Map<Long, ProjectLineEntity> lineMap = projectLineRepository.getListByProjectId(projectId).stream()
                .collect(Collectors.toMap(ProjectLineEntity::getId, l -> l));
        return toProjectPaymentDto(entity, lineMap);
    }

    @Override
    @Transactional
    public void deleteProjectPayment(Long projectId, Long paymentId) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);
        ProjectPaymentEntity entity = projectPaymentRepository.findById(paymentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST002");
        }
        projectPaymentRepository.delete(entity);
        recalculateAndSaveProjectRemaining(project);
    }

    private ProjectPaymentDto toProjectPaymentDto(ProjectPaymentEntity entity,
                                                  Map<Long, ProjectLineEntity> lineMap) {
        ProjectPaymentDto dto = projectPaymentMapper.toDto(entity);
        if (entity.getPaymentMethod() != null) {
            dto.setPaymentMethodLabel(entity.getPaymentMethod().getLabel());
        }
        if (entity.getStatus() != null) {
            dto.setStatusLabel(entity.getStatus().getLabel());
        }
        if (entity.getProjectLineId() != null && lineMap.containsKey(entity.getProjectLineId())) {
            ProjectLineEntity line = lineMap.get(entity.getProjectLineId());
            dto.setPaymentPlanNo(line.getPaymentNo());
            dto.setPaymentPlanMilestone(line.getConditionMilestone());
        }
        return dto;
    }

    // =========================================================================
    // 2.15: QUẢN LÝ TÀI LIỆU DỰ ÁN
    // =========================================================================

    @Override
    public List<ProjectDocumentDto> getProjectDocuments(Long projectId, ProjectDocumentFolder folder) {
        checkProjectAccessScope(projectId);
        projectRepository.getProjectById(projectId);
        List<ProjectDocumentEntity> entities = folder != null
                ? projectDocumentRepository.getListByProjectIdAndFolder(projectId, folder)
                : projectDocumentRepository.getListByProjectId(projectId);
        return entities.stream().map(this::toProjectDocumentDto).toList();
    }

    @Override
    @Transactional
    public ProjectDocumentDto addProjectDocument(Long projectId, ProjectDocumentDto input) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);

        ProjectDocumentEntity entity = projectDocumentMapper.toEntity(input);
        entity.setId(null);
        entity.setProjectId(projectId);
        entity.setFolder(input.getFolder() != null ? input.getFolder() : ProjectDocumentFolder.DRAWING);
        entity.setName(StringUtils.trimToEmpty(input.getName()));
        entity.setFileName(StringUtils.trimToNull(input.getFileName()));
        entity.setFileUrl(StringUtils.trimToEmpty(input.getFileUrl()));
        entity.setVersion(StringUtils.isNotBlank(input.getVersion()) ? input.getVersion().trim() : "v1.0");
        entity.setDescription(StringUtils.trimToNull(input.getDescription()));
        entity.setUploadedAt(LocalDateTime.now());
        entity.setUploadedByName(resolveCurrentUploaderName());

        projectDocumentRepository.save(entity);

        auditLogService.logExplicit(
                "UPLOAD_PROJECT_DOCUMENT",
                "PROJECT",
                "ProjectDocumentEntity",
                String.valueOf(entity.getId()),
                null,
                Map.of(
                        "folder", entity.getFolder().name(),
                        "name", entity.getName(),
                        "version", entity.getVersion()
                ),
                "Tải lên tài liệu \"" + entity.getName() + "\" (" + entity.getVersion() + ") vào thư mục "
                        + entity.getFolder().getLabel() + " cho dự án " + project.getCode(),
                null
        );

        return toProjectDocumentDto(entity);
    }

    @Override
    @Transactional
    public ProjectDocumentDto updateProjectDocument(Long projectId, Long documentId, ProjectDocumentDto input) {
        checkProjectAccessScope(projectId);
        projectRepository.getProjectById(projectId);
        ProjectDocumentEntity entity = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST002");
        }

        if (input.getFolder() != null) entity.setFolder(input.getFolder());
        if (StringUtils.isNotBlank(input.getName())) entity.setName(input.getName().trim());
        if (StringUtils.isNotBlank(input.getFileUrl())) entity.setFileUrl(input.getFileUrl().trim());
        if (input.getFileName() != null) entity.setFileName(StringUtils.trimToNull(input.getFileName()));
        if (input.getFileType() != null) entity.setFileType(StringUtils.trimToNull(input.getFileType()));
        if (input.getFileSize() != null) entity.setFileSize(input.getFileSize());
        if (StringUtils.isNotBlank(input.getVersion())) entity.setVersion(input.getVersion().trim());
        entity.setDescription(StringUtils.trimToNull(input.getDescription()));
        entity.setUploadedAt(LocalDateTime.now());
        entity.setUploadedByName(resolveCurrentUploaderName());

        projectDocumentRepository.save(entity);
        return toProjectDocumentDto(entity);
    }

    @Override
    @Transactional
    public void deleteProjectDocument(Long projectId, Long documentId) {
        checkProjectAccessScope(projectId);
        projectRepository.getProjectById(projectId);
        ProjectDocumentEntity entity = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getProjectId(), projectId)) {
            throw BusinessException.exception("CST002");
        }
        projectDocumentRepository.delete(entity);
    }

    private ProjectDocumentDto toProjectDocumentDto(ProjectDocumentEntity entity) {
        ProjectDocumentDto dto = projectDocumentMapper.toDto(entity);
        if (entity.getFolder() != null) {
            dto.setFolderLabel(entity.getFolder().getLabel());
        }
        return dto;
    }

    private String resolveCurrentUploaderName() {
        try {
            Long staffId = getCurrentStaffId();
            if (staffId != null) {
                return staffRepository.findById(staffId)
                        .map(StaffEntity::getName)
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> SecurityUtils.getCurrentUserLogin().orElse("Hệ thống"));
            }
            return SecurityUtils.getCurrentUserLogin().orElse("Hệ thống");
        } catch (Exception e) {
            return "Hệ thống";
        }
    }

    // =========================================================================
    // 2.19: KHỐI LƯỢNG & ĐƠN GIÁ HẠNG MỤC THEO DỰ ÁN
    // =========================================================================

    @Override
    @Transactional
    public List<ProjectCategoryMapDto> saveProjectCategories(Long projectId, List<ProjectCategoryMapDto> categories) {
        checkProjectAccessScope(projectId);
        ProjectEntity project = projectRepository.getProjectById(projectId);

        List<ProjectCategoryMapEntity> oldMaps = projectCategoryMapRepository.getListByProjectId(projectId);
        if (!oldMaps.isEmpty()) {
            projectCategoryMapRepository.deleteAllInBatch(oldMaps);
        }
        saveProjectCategoryMaps(project, categories);

        ProjectDto temp = ProjectDto.builder().id(projectId).build();
        setProjectCategoryMaps(temp, projectCategoryMapRepository.getListByProjectId(projectId));
        return temp.getProjectCategoryMaps();
    }

}
