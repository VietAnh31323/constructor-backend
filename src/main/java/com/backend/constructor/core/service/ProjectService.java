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
import com.backend.constructor.core.port.mapper.ProjectLineMapper;
import com.backend.constructor.core.port.mapper.ProjectMemberMapper;
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
        projectEntity.setRemainingAmount(calculateRemainingAmount(contractVal, projectEntity.getContractAdvance()));

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
        projectEntity.setRemainingAmount(calculateRemainingAmount(contractVal, projectEntity.getContractAdvance()));

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
        deleteAllRelations(projectEntity);
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

        // Load phases (project_progress)
        List<ProjectProgressEntity> ppEntities = new ArrayList<>(projectProgressRepository.getListByProjectId(projectId));
        ppEntities.sort(Comparator.comparing(ProjectProgressEntity::getId));

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
        for (TaskStaffMapEntity m : allTaskStaffMaps) {
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

        List<LocalDate> candidateDates = new ArrayList<>();
        if (project.getSignDate() != null) candidateDates.add(project.getSignDate());
        if (project.getDeliveryDate() != null) candidateDates.add(project.getDeliveryDate());
        if (project.getActualHandoverDate() != null) candidateDates.add(project.getActualHandoverDate());

        int totalTasksCount = 0;
        int completedTasksCount = 0;
        int inProgressTasksCount = 0;
        int notStartedTasksCount = 0;
        int overdueTasksCount = 0;

        List<ProjectGanttDto.PhaseItem> phaseItems = new ArrayList<>();
        for (ProjectProgressEntity pp : ppEntities) {
            CodeNameResponse progInfo = progressCatalogMap.get(pp.getProgressId());
            List<Long> mappedTaskIds = taskIdsByPpId.getOrDefault(pp.getId(), Collections.emptyList());

            List<ProjectGanttDto.TaskItem> phaseTaskDtos = new ArrayList<>();
            LocalDate phaseStart = null;
            LocalDate phaseEnd = null;
            int phaseLeafTotal = 0;
            int phaseLeafCompleted = 0;
            BigDecimal phasePctSum = BigDecimal.ZERO;

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
                    ProjectGanttDto.TaskItem subItem = toGanttTaskItem(
                            sub, pp.getId(), staffSimpleMap, assigneesByTaskId, Collections.emptyList(), today
                    );
                    subDtos.add(subItem);
                    if (subItem.getStartDate() != null) candidateDates.add(subItem.getStartDate());
                    if (subItem.getEndDate() != null) candidateDates.add(subItem.getEndDate());

                    totalTasksCount++;
                    if (subItem.getState() == TaskState.COMPLETED) completedTasksCount++;
                    else if (subItem.getState() == TaskState.IN_PROGRESS) inProgressTasksCount++;
                    else notStartedTasksCount++;
                    if (subItem.isOverdue()) overdueTasksCount++;
                }

                ProjectGanttDto.TaskItem rootItem = toGanttTaskItem(
                        rootTask, pp.getId(), staffSimpleMap, assigneesByTaskId, subDtos, today
                );

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

                // Count root task in KPIs if it has no subTasks (leaf work item)
                if (subDtos.isEmpty()) {
                    totalTasksCount++;
                    if (rootItem.getState() == TaskState.COMPLETED) completedTasksCount++;
                    else if (rootItem.getState() == TaskState.IN_PROGRESS) inProgressTasksCount++;
                    else notStartedTasksCount++;
                    if (rootItem.isOverdue()) overdueTasksCount++;
                }

                phaseLeafTotal++;
                if (rootItem.getState() == TaskState.COMPLETED) {
                    phaseLeafCompleted++;
                }
                phasePctSum = phasePctSum.add(rootItem.getProgressPercent() != null ? rootItem.getProgressPercent() : BigDecimal.ZERO);

                phaseTaskDtos.add(rootItem);
            }

            BigDecimal phasePct = phaseLeafTotal > 0
                    ? phasePctSum.divide(BigDecimal.valueOf(phaseLeafTotal), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            phaseItems.add(ProjectGanttDto.PhaseItem.builder()
                    .projectProgressId(pp.getId())
                    .progressId(pp.getProgressId())
                    .progressCode(progInfo != null ? progInfo.getCode() : null)
                    .progressName(progInfo != null ? progInfo.getName() : "Giai đoạn thi công")
                    .startDate(phaseStart)
                    .endDate(phaseEnd)
                    .progressPercent(phasePct)
                    .taskCount(phaseTaskDtos.size())
                    .completedTaskCount(phaseLeafCompleted)
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

        return ProjectGanttDto.builder()
                .projectId(project.getId())
                .projectCode(project.getCode())
                .projectName(project.getName())
                .customerName(customerName)
                .managerName(managerName)
                .state(currentState)
                .stateLabel(currentState.getLabel())
                .progressPercent(project.getProgressPercent() != null ? project.getProgressPercent() : BigDecimal.ZERO)
                .signDate(project.getSignDate())
                .deliveryDate(project.getDeliveryDate())
                .actualHandoverDate(project.getActualHandoverDate())
                .warrantyEndDate(project.getWarrantyEndDate())
                .timelineStartDate(minDate.minusDays(3))
                .timelineEndDate(maxDate.plusDays(5))
                .totalPhases(phaseItems.size())
                .totalTasks(totalTasksCount)
                .completedTasks(completedTasksCount)
                .inProgressTasks(inProgressTasksCount)
                .notStartedTasks(notStartedTasksCount)
                .overdueTasks(overdueTasksCount)
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

        ProjectProgressEntity ppEntity = ProjectProgressEntity.builder()
                .projectId(project.getId())
                .progressId(targetProgressId)
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
        List<ProjectProgressTaskMapEntity> maps = projectProgressTaskMapRepository
                .getListByProjectProgressIds(Set.of(projectProgressId));
        if (!maps.isEmpty()) {
            projectProgressTaskMapRepository.deleteAll(maps);
        }
        projectProgressRepository.delete(ppEntity);
        recalculateProjectProgressPercent(projectId);
    }

    @Override
    @Transactional
    public IdResponse addGanttTask(Long projectId, ProjectGanttTaskRequestDto input) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        input.trimData();
        if (input.getEndDate().isBefore(input.getStartDate())) {
            throw BusinessException.exception("ERROR_0001");
        }

        TaskState initialState = input.getState() != null ? input.getState() : TaskState.NOT_STARTED;
        BigDecimal pct = input.getProgressPercent();
        if (pct == null) {
            pct = switch (initialState) {
                case COMPLETED -> BigDecimal.valueOf(100);
                case IN_PROGRESS -> BigDecimal.valueOf(50);
                default -> BigDecimal.ZERO;
            };
        }

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
            TaskEntity parentTask = taskRepository.getTaskById(input.getParentTaskId());
            List<TaskEntity> siblings = taskRepository.getListTaskByParentId(parentTask.getId());
            if (!siblings.isEmpty()) {
                long doneCount = siblings.stream().filter(s -> s.getState() == TaskState.COMPLETED).count();
                BigDecimal parentPct = BigDecimal.valueOf((double) doneCount * 100.0 / siblings.size())
                        .setScale(2, RoundingMode.HALF_UP);
                parentTask.setProgressPercent(parentPct);
                if (doneCount == siblings.size()) {
                    parentTask.setState(TaskState.COMPLETED);
                } else if (siblings.stream().anyMatch(s -> s.getState() != TaskState.NOT_STARTED)) {
                    parentTask.setState(TaskState.IN_PROGRESS);
                } else {
                    parentTask.setState(TaskState.NOT_STARTED);
                }
                taskRepository.save(parentTask);
            }
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

        recalculateProjectProgressPercent(projectId);
        return IdResponse.builder().id(taskEntity.getId()).build();
    }

    private ProjectGanttDto.TaskItem toGanttTaskItem(TaskEntity entity,
                                                     Long projectProgressId,
                                                     Map<Long, CodeNameResponse> staffMap,
                                                     Map<Long, List<CodeNameResponse>> assigneesMap,
                                                     List<ProjectGanttDto.TaskItem> subTasks,
                                                     LocalDate today) {
        TaskState st = entity.getState() != null ? entity.getState() : TaskState.NOT_STARTED;
        BigDecimal pct = entity.getProgressPercent();
        if (pct == null) {
            pct = st == TaskState.COMPLETED ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        long duration = 1L;
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            duration = Math.max(1L, ChronoUnit.DAYS.between(entity.getStartDate(), entity.getEndDate()) + 1L);
        }
        Long remain = null;
        boolean overdue = false;
        if (entity.getEndDate() != null) {
            remain = ChronoUnit.DAYS.between(today, entity.getEndDate());
            overdue = st != TaskState.COMPLETED && entity.getEndDate().isBefore(today);
        }

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
                .priorityLevel(entity.getPriorityLevel())
                .overdue(overdue)
                .reviewer(entity.getReviewerId() != null ? staffMap.get(entity.getReviewerId()) : null)
                .assignees(assigneesMap.getOrDefault(entity.getId(), Collections.emptyList()))
                .subTasks(subTasks)
                .build();
    }

    private void recalculateProjectProgressPercent(Long projectId) {
        ProjectEntity project = projectRepository.getProjectById(projectId);
        List<TaskEntity> rootTasks = taskRepository.getListTaskByProjectId(projectId);
        if (rootTasks.isEmpty()) {
            return;
        }
        BigDecimal sum = rootTasks.stream()
                .map(t -> {
                    if (t.getProgressPercent() != null) return t.getProgressPercent();
                    return t.getState() == TaskState.COMPLETED ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(rootTasks.size()), 2, RoundingMode.HALF_UP);
        project.setProgressPercent(avg);
        projectRepository.save(project);
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
        if (projectLines == null || projectLines.isEmpty()) {
            return;
        }
        List<ProjectLineEntity> projectLineEntities = new ArrayList<>();
        for (ProjectLineDto projectLineDto : projectLines) {
            if (projectLineDto == null) continue;
            ProjectLineEntity entity = projectLineMapper.toEntity(projectLineDto);
            if (entity.getPaymentAmount() != null) {
                entity.setPaymentAmount(entity.getPaymentAmount().setScale(2, RoundingMode.HALF_UP));
            }
            entity.setProjectId(projectEntity.getId());
            projectLineEntities.add(entity);
        }
        if (!projectLineEntities.isEmpty()) {
            projectLineRepository.saveAll(projectLineEntities);
        }
    }

    private void saveProjectCategoryMaps(ProjectEntity projectEntity,
                                         List<ProjectCategoryMapDto> projectCategoryMaps) {
        if (projectCategoryMaps == null || projectCategoryMaps.isEmpty()) {
            return;
        }
        List<ProjectCategoryMapEntity> projectCategoryMapEntities = new ArrayList<>();
        for (ProjectCategoryMapDto projectCategoryMap : projectCategoryMaps) {
            CodeNameResponse category = projectCategoryMap.getCategory();
            if (category == null || category.getId() == null) {
                throw BusinessException.exception("CST015");
            }
            ProjectCategoryMapEntity projectCategoryMapEntity = ProjectCategoryMapEntity.builder()
                    .projectId(projectEntity.getId())
                    .categoryId(category.getId())
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
                    .build();
            projectCategoryMaps.add(projectCategoryMapDto);
        }
        projectDto.setProjectCategoryMaps(projectCategoryMaps);
    }

    private void setProjectCategoryLines(ProjectDto projectDto,
                                         List<ProjectLineEntity> projectLineEntities) {
        List<ProjectLineDto> projectLines = projectLineEntities.stream()
                .map(projectLineMapper::toDto)
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

    private BigDecimal calculateRemainingAmount(BigDecimal contractValue,
                                                BigDecimal contractAdvance) {
        BigDecimal value = contractValue != null ? contractValue : BigDecimal.ZERO;
        BigDecimal advance = contractAdvance != null ? contractAdvance : BigDecimal.ZERO;
        return value.subtract(advance).setScale(2, RoundingMode.HALF_UP);
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
            project.setRemainingAmount(calculateRemainingAmount(entity.getValueAfterTax(), entity.getAdvanceAmount()));
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
            project.setRemainingAmount(calculateRemainingAmount(entity.getValueAfterTax(), entity.getAdvanceAmount()));
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

}
