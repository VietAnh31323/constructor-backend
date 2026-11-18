package com.backend.constructor.core.service;

import com.backend.constructor.app.api.ProjectProgressApi;
import com.backend.constructor.app.dto.project_progress.PhaseReorderDto;
import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.app.dto.project_progress.ProjectPhaseDto;
import com.backend.constructor.app.dto.project_progress.ProjectProgressDto;
import com.backend.constructor.app.dto.project_progress.ProjectProgressSummaryDto;
import com.backend.constructor.app.dto.task.ReviewerDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.domain.enums.TaskState;
import com.backend.constructor.core.port.mapper.ProjectMapper;
import com.backend.constructor.core.port.mapper.TaskMapper;
import com.backend.constructor.core.port.repository.*;
import com.backend.constructor.core.service.dto.DataCollect;
import com.backend.constructor.core.service.internal.InternalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.backend.constructor.core.service.HelperService.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectProgressService implements ProjectProgressApi {
    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final ProjectProgressRepository projectProgressRepository;
    private final ProjectProgressTaskMapRepository projectProgressTaskMapRepository;
    private final StaffRepository staffRepository;
    private final ProjectMapper projectMapper;
    private final ProgressRepository progressRepository;
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final InternalTaskService internalTaskService;
    private final ProgressCalculationService progressCalculationService;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;
    private final CurrentUserService currentUserService;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    @Transactional
    public IdResponse create(ProjectInfoDto input) {
        ProjectEntity project = projectRepository.getProjectById(input.getId());
        List<ProjectProgressTaskMapEntity> progressTaskMapEntities = new ArrayList<>();
        int nextOrder = nextSortOrder(project.getId());
        for (ProjectProgressDto projectProgress : input.getProjectProgress()) {
            CodeNameResponse progress = projectProgress.getProgress();
            if (progress == null) continue;
            ProjectProgressEntity projectProgressEntity = ProjectProgressEntity.builder()
                    .projectId(project.getId())
                    .progressId(progress.getId())
                    .sortOrder(nextOrder++)
                    .weight(BigDecimal.ONE)
                    .progressPercent(BigDecimal.ZERO)
                    .build();
            projectProgressRepository.save(projectProgressEntity);
            addAllIfNotNull(progressTaskMapEntities, getProgressTaskMapEntities(projectProgressEntity.getId(), projectProgress.getTasks()));
        }
        projectProgressTaskMapRepository.saveAll(progressTaskMapEntities);
        progressCalculationService.recalculateProject(project.getId());
        return IdResponse.builder().id(project.getId()).build();
    }

    // =========================================================================
    // 2.21: QUẢN LÝ GIAI ĐOẠN – SỬA / XÓA / SẮP XẾP, NGÀY KẾ HOẠCH & THỰC TẾ
    // =========================================================================

    @Override
    public List<ProjectPhaseDto> getPhases(Long projectId) {
        checkProjectAccess(projectId);
        projectRepository.getProjectById(projectId);
        List<ProjectProgressEntity> phases = sortedPhases(projectId);
        if (phases.isEmpty()) return Collections.emptyList();

        Set<Long> phaseIds = phases.stream().map(ProjectProgressEntity::getId).collect(Collectors.toSet());
        Set<Long> progressIds = phases.stream().map(ProjectProgressEntity::getProgressId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CodeNameResponse> catalog = progressIds.isEmpty()
                ? Collections.emptyMap()
                : progressRepository.getMapProgressSimpleByIds(progressIds);

        Map<Long, List<TaskEntity>> leavesByPhase = loadLeafTasksByPhase(phaseIds);
        LocalDate today = LocalDate.now();
        List<ProjectPhaseDto> result = new ArrayList<>();
        for (ProjectProgressEntity phase : phases) {
            result.add(toPhaseDto(phase, catalog.get(phase.getProgressId()),
                    leavesByPhase.getOrDefault(phase.getId(), Collections.emptyList()), today));
        }
        return result;
    }

    @Override
    @Transactional
    public ProjectPhaseDto updatePhase(Long projectProgressId, ProjectPhaseDto input) {
        ProjectProgressEntity phase = projectProgressRepository.findById(projectProgressId)
                .orElseThrow(() -> BusinessException.exception("CST064"));
        checkProjectAccess(phase.getProjectId());

        if (input.getPlannedStartDate() != null && input.getPlannedEndDate() != null
                && input.getPlannedEndDate().isBefore(input.getPlannedStartDate())) {
            throw BusinessException.exception("CST063");
        }

        String newName = StringUtils.trimToNull(input.getNewProgressName());
        if (newName != null) {
            ProgressEntity newProgress = ProgressEntity.builder()
                    .name(newName)
                    .isActive(true)
                    .build();
            generateCodeService.generateCode(newProgress, Constants.TT, ProgressEntity.class);
            uniqueValidationService.validate(newProgress);
            progressRepository.save(newProgress);
            phase.setProgressId(newProgress.getId());
        } else if (input.getProgressId() != null && !Objects.equals(input.getProgressId(), phase.getProgressId())) {
            progressRepository.getProgressById(input.getProgressId());
            phase.setProgressId(input.getProgressId());
        }

        phase.setPlannedStartDate(input.getPlannedStartDate());
        phase.setPlannedEndDate(input.getPlannedEndDate());
        if (input.getWeight() != null) {
            phase.setWeight(ProgressCalculationService.weightOf(input.getWeight()).setScale(2, RoundingMode.HALF_UP));
        }
        phase.setNote(StringUtils.trimToNull(input.getNote()));
        if (input.getSortOrder() != null) {
            phase.setSortOrder(input.getSortOrder());
        }
        projectProgressRepository.save(phase);
        progressCalculationService.recalculateProject(phase.getProjectId());

        return getPhases(phase.getProjectId()).stream()
                .filter(p -> Objects.equals(p.getProjectProgressId(), projectProgressId))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public void deletePhase(Long projectProgressId) {
        ProjectProgressEntity phase = projectProgressRepository.findById(projectProgressId)
                .orElseThrow(() -> BusinessException.exception("CST064"));
        Long projectId = phase.getProjectId();
        checkProjectAccess(projectId);

        List<ProjectProgressTaskMapEntity> maps = projectProgressTaskMapRepository
                .getListByProjectProgressIds(Set.of(projectProgressId));
        Set<Long> rootIds = maps.stream().map(ProjectProgressTaskMapEntity::getTaskId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // Xóa toàn bộ cây công việc thuộc giai đoạn
        Set<Long> allTaskIds = new HashSet<>(rootIds);
        Set<Long> frontier = new HashSet<>(rootIds);
        int guard = 0;
        while (!frontier.isEmpty() && guard++ < 10) {
            Set<Long> next = taskRepository.getListTaskByParentIds(frontier).stream()
                    .map(TaskEntity::getId)
                    .filter(id -> !allTaskIds.contains(id))
                    .collect(Collectors.toSet());
            allTaskIds.addAll(next);
            frontier = next;
        }
        if (!maps.isEmpty()) {
            projectProgressTaskMapRepository.deleteAll(maps);
        }
        if (!allTaskIds.isEmpty()) {
            taskStaffMapRepository.deleteAllByTaskIds(allTaskIds);
            taskRepository.deleteAll(taskRepository.findAllByIds(allTaskIds));
        }
        projectProgressRepository.delete(phase);

        // Đánh lại thứ tự liên tục
        List<ProjectProgressEntity> remaining = sortedPhases(projectId);
        int order = 1;
        for (ProjectProgressEntity p : remaining) {
            p.setSortOrder(order++);
        }
        projectProgressRepository.saveAll(remaining);
        progressCalculationService.recalculateProject(projectId);
    }

    @Override
    @Transactional
    public List<ProjectPhaseDto> reorderPhases(Long projectId, PhaseReorderDto input) {
        checkProjectAccess(projectId);
        List<ProjectProgressEntity> phases = projectProgressRepository.getListByProjectId(projectId);
        Map<Long, ProjectProgressEntity> byId = phases.stream()
                .collect(Collectors.toMap(ProjectProgressEntity::getId, p -> p));
        int order = 1;
        Set<Long> seen = new HashSet<>();
        for (Long id : input.getOrderedIds()) {
            ProjectProgressEntity p = byId.get(id);
            if (p == null || !seen.add(id)) continue;
            p.setSortOrder(order++);
        }
        // Giai đoạn không có trong danh sách gửi lên sẽ được xếp cuối
        for (ProjectProgressEntity p : phases) {
            if (!seen.contains(p.getId())) {
                p.setSortOrder(order++);
            }
        }
        projectProgressRepository.saveAll(phases);
        return getPhases(projectId);
    }

    @Override
    @Transactional
    public BigDecimal recalculate(Long projectId) {
        checkProjectAccess(projectId);
        return progressCalculationService.recalculateProject(projectId);
    }

    // =========================================================================
    // 2.24: TỔNG HỢP TIẾN ĐỘ CHO MÀN QUẢN LÝ TIẾN ĐỘ
    // =========================================================================

    @Override
    public List<ProjectProgressSummaryDto> getSummaries(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyList();
        LocalDate today = LocalDate.now();
        List<ProjectProgressSummaryDto> result = new ArrayList<>();
        Map<Long, ProjectEntity> projects = projectRepository.findAllByIds(new HashSet<>(projectIds)).stream()
                .collect(Collectors.toMap(ProjectEntity::getId, p -> p));
        for (Long projectId : new LinkedHashSet<>(projectIds)) {
            ProjectEntity project = projects.get(projectId);
            if (project == null) continue;
            List<ProjectProgressEntity> phases = projectProgressRepository.getListByProjectId(projectId);
            Set<Long> phaseIds = phases.stream().map(ProjectProgressEntity::getId).collect(Collectors.toSet());
            List<TaskEntity> leaves = loadLeafTasksByPhase(phaseIds).values().stream()
                    .flatMap(List::stream).toList();

            int notStarted = 0, inProgress = 0, pending = 0, returned = 0, completed = 0, overdue = 0;
            LocalDate nextDeadline = null;
            for (TaskEntity t : leaves) {
                TaskState st = t.getState() != null ? t.getState() : TaskState.NOT_STARTED;
                switch (st) {
                    case COMPLETED -> completed++;
                    case PENDING_REVIEW -> pending++;
                    case RETURNED -> returned++;
                    case IN_PROGRESS -> inProgress++;
                    default -> notStarted++;
                }
                if (st != TaskState.COMPLETED && t.getEndDate() != null) {
                    if (t.getEndDate().isBefore(today)) {
                        overdue++;
                    } else if (nextDeadline == null || t.getEndDate().isBefore(nextDeadline)) {
                        nextDeadline = t.getEndDate();
                    }
                }
            }
            LocalDate plannedEnd = phases.stream().map(ProjectProgressEntity::getPlannedEndDate)
                    .filter(Objects::nonNull).max(LocalDate::compareTo)
                    .orElse(project.getDeliveryDate());
            boolean allDone = !leaves.isEmpty() && completed == leaves.size();
            LocalDate actualEnd = allDone
                    ? leaves.stream().map(TaskEntity::getActualEndDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null)
                    : null;
            BigDecimal pct = project.getProgressPercent() != null ? project.getProgressPercent() : BigDecimal.ZERO;
            String status = ScheduleStatusUtil.compute(plannedEnd, actualEnd, allDone, pct, today);
            if (overdue > 0 && !allDone && ScheduleStatusUtil.ON_TRACK.equals(status)) {
                status = ScheduleStatusUtil.AT_RISK;
            }
            result.add(ProjectProgressSummaryDto.builder()
                    .projectId(projectId)
                    .progressPercent(pct)
                    .phaseCount(phases.size())
                    .totalTasks(leaves.size())
                    .notStartedTasks(notStarted)
                    .inProgressTasks(inProgress)
                    .pendingReviewTasks(pending)
                    .returnedTasks(returned)
                    .completedTasks(completed)
                    .overdueTasks(overdue)
                    .plannedEndDate(plannedEnd)
                    .nextDeadline(nextDeadline)
                    .delayDays(ScheduleStatusUtil.delayDays(plannedEnd, actualEnd, allDone, today))
                    .scheduleStatus(status)
                    .build());
        }
        return result;
    }

    // ------------------------------ helpers ------------------------------

    private List<ProjectProgressEntity> sortedPhases(Long projectId) {
        List<ProjectProgressEntity> phases = new ArrayList<>(projectProgressRepository.getListByProjectId(projectId));
        phases.sort(Comparator.comparing((ProjectProgressEntity p) -> p.getSortOrder() != null ? p.getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(ProjectProgressEntity::getId));
        return phases;
    }

    private int nextSortOrder(Long projectId) {
        return projectProgressRepository.getListByProjectId(projectId).stream()
                .map(ProjectProgressEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    /** Lấy các công việc lá (không có việc con) thuộc từng giai đoạn. */
    private Map<Long, List<TaskEntity>> loadLeafTasksByPhase(Set<Long> phaseIds) {
        if (phaseIds == null || phaseIds.isEmpty()) return Collections.emptyMap();
        List<ProjectProgressTaskMapEntity> maps = projectProgressTaskMapRepository.getListByProjectProgressIds(phaseIds);
        Map<Long, Long> phaseByRoot = new HashMap<>();
        for (ProjectProgressTaskMapEntity m : maps) {
            if (m.getTaskId() != null && m.getProjectProgressId() != null) {
                phaseByRoot.put(m.getTaskId(), m.getProjectProgressId());
            }
        }
        if (phaseByRoot.isEmpty()) return Collections.emptyMap();

        Map<Long, TaskEntity> all = new HashMap<>();
        Map<Long, Long> phaseOfTask = new HashMap<>(phaseByRoot);
        taskRepository.findAllByIds(phaseByRoot.keySet()).forEach(t -> all.put(t.getId(), t));
        Set<Long> parents = new HashSet<>();
        Set<Long> frontier = new HashSet<>(all.keySet());
        int guard = 0;
        while (!frontier.isEmpty() && guard++ < 10) {
            List<TaskEntity> children = taskRepository.getListTaskByParentIds(frontier);
            frontier = new HashSet<>();
            for (TaskEntity c : children) {
                if (all.containsKey(c.getId())) continue;
                all.put(c.getId(), c);
                parents.add(c.getParentId());
                phaseOfTask.put(c.getId(), phaseOfTask.get(c.getParentId()));
                frontier.add(c.getId());
            }
        }
        Map<Long, List<TaskEntity>> result = new HashMap<>();
        for (TaskEntity t : all.values()) {
            if (parents.contains(t.getId())) continue;
            Long phaseId = phaseOfTask.get(t.getId());
            if (phaseId != null) {
                result.computeIfAbsent(phaseId, k -> new ArrayList<>()).add(t);
            }
        }
        return result;
    }

    private ProjectPhaseDto toPhaseDto(ProjectProgressEntity phase,
                                       CodeNameResponse catalog,
                                       List<TaskEntity> leaves,
                                       LocalDate today) {
        int completed = 0, pending = 0, overdue = 0;
        for (TaskEntity t : leaves) {
            if (t.getState() == TaskState.COMPLETED) completed++;
            else {
                if (t.getState() == TaskState.PENDING_REVIEW) pending++;
                if (t.getEndDate() != null && t.getEndDate().isBefore(today)) overdue++;
            }
        }
        LocalDate plannedEnd = phase.getPlannedEndDate() != null
                ? phase.getPlannedEndDate()
                : leaves.stream().map(TaskEntity::getEndDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        boolean allDone = !leaves.isEmpty() && completed == leaves.size();
        BigDecimal pct = phase.getProgressPercent() != null ? phase.getProgressPercent() : BigDecimal.ZERO;
        return ProjectPhaseDto.builder()
                .projectProgressId(phase.getId())
                .projectId(phase.getProjectId())
                .progressId(phase.getProgressId())
                .progressCode(catalog != null ? catalog.getCode() : null)
                .progressName(catalog != null ? catalog.getName() : "Giai đoạn thi công")
                .sortOrder(phase.getSortOrder())
                .plannedStartDate(phase.getPlannedStartDate())
                .plannedEndDate(phase.getPlannedEndDate())
                .actualStartDate(phase.getActualStartDate())
                .actualEndDate(phase.getActualEndDate())
                .weight(ProgressCalculationService.weightOf(phase.getWeight()))
                .progressPercent(pct)
                .note(phase.getNote())
                .taskCount(leaves.size())
                .completedTaskCount(completed)
                .pendingReviewTaskCount(pending)
                .overdueTaskCount(overdue)
                .delayDays(ScheduleStatusUtil.delayDays(plannedEnd, phase.getActualEndDate(), allDone, today))
                .scheduleStatus(ScheduleStatusUtil.compute(plannedEnd, phase.getActualEndDate(), allDone, pct, today))
                .build();
    }

    private void checkProjectAccess(Long projectId) {
        if (projectId == null || currentUserService.isAdmin()) return;
        Long staffId = currentUserService.getCurrentStaffId();
        if (staffId == null) return;
        ProjectEntity p = projectRepository.getProjectById(projectId);
        boolean participant = Objects.equals(p.getManagerId(), staffId)
                || Objects.equals(p.getCreatorId(), staffId)
                || Objects.equals(p.getSupporterId(), staffId)
                || projectMemberRepository.findByProjectIdAndStaffId(projectId, staffId).isPresent();
        if (!participant) {
            throw BusinessException.exception("CST050");
        }
    }

    @Override
    public ProjectInfoDto getDetail(Long id) {
        ProjectEntity project = projectRepository.getProjectById(id);
        Set<Long> staffIds = new HashSet<>();
        ProjectInfoDto projectInfoDto = projectMapper.toInfoDto(project);
        if (project.getCustomerId() != null) {
            customerRepository.findById(project.getCustomerId()).ifPresent(cust -> {
                projectInfoDto.setCustomerId(cust.getId());
                projectInfoDto.setCustomer(CodeNameResponse.builder()
                        .id(cust.getId())
                        .code(cust.getCode())
                        .name(cust.getName())
                        .build());
                projectInfoDto.setOwner(cust.getName());
            });
        }
        addIfNotNull(staffIds, project.getCreatorId());
        addIfNotNull(staffIds, project.getManagerId());
        addIfNotNull(staffIds, project.getSupporterId());
        Map<Long, CodeNameResponse> staffMap = staffRepository.getMapSimpleStaffByIds(staffIds);
        projectInfoDto.setCreator(staffMap.get(project.getCreatorId()));
        projectInfoDto.setManager(staffMap.get(project.getManagerId()));
        projectInfoDto.setSupporter(staffMap.get(project.getSupporterId()));
        projectInfoDto.setProjectProgress(getProjectProgress(projectProgressRepository.getListByProjectId(project.getId())));
        return projectInfoDto;
    }

    private List<ProjectProgressTaskMapEntity> getProgressTaskMapEntities(Long projectProgressId,
                                                                          List<TaskOutput> tasks) {
        if (CollectionUtils.isEmpty(tasks)) return new ArrayList<>();
        List<ProjectProgressTaskMapEntity> projectProgressTaskMapEntities = new ArrayList<>();
        for (TaskOutput task : tasks) {
            ProjectProgressTaskMapEntity projectProgressTaskMap = ProjectProgressTaskMapEntity.builder()
                    .projectProgressId(projectProgressId)
                    .taskId(task.getId())
                    .build();
            projectProgressTaskMapEntities.add(projectProgressTaskMap);
        }
        return projectProgressTaskMapEntities;
    }

    private List<ProjectProgressDto> getProjectProgress(List<ProjectProgressEntity> projectProgressEntities) {
        Set<Long> projectProgressIds = new HashSet<>();
        Set<Long> progressIds = new HashSet<>();
        for (ProjectProgressEntity projectProgressEntity : projectProgressEntities) {
            addIfNotNull(projectProgressIds, projectProgressEntity.getId());
            addIfNotNull(progressIds, projectProgressEntity.getProgressId());
        }
        Map<Long, CodeNameResponse> progressMap = progressRepository.getMapProgressSimpleByIds(progressIds);
        Map<Long, List<TaskOutput>> taskOutputMap = getTaskOutputMapGroupByProgressId(projectProgressTaskMapRepository.getListByProjectProgressIds(projectProgressIds));
        List<ProjectProgressDto> projectProgress = new ArrayList<>();
        for (ProjectProgressEntity projectProgressEntity : projectProgressEntities) {
            ProjectProgressDto projectProgressDto = ProjectProgressDto.builder()
                    .progress(getData(progressMap, projectProgressEntity.getProgressId()))
                    .tasks(getData(taskOutputMap, projectProgressEntity.getId()))
                    .build();
            projectProgress.add(projectProgressDto);
        }
        return projectProgress;
    }

    private Map<Long, List<TaskOutput>> getTaskOutputMapGroupByProgressId(List<ProjectProgressTaskMapEntity> progressTaskMapEntities) {
        Set<Long> taskIds = new HashSet<>();
        for (ProjectProgressTaskMapEntity progressTaskMapEntity : progressTaskMapEntities) {
            addIfNotNull(taskIds, progressTaskMapEntity.getTaskId());
        }
        List<TaskEntity> taskEntities = taskRepository.findAllByIds(taskIds);
        List<TaskStaffMapEntity> taskStaffMapEntities = taskStaffMapRepository.getListByTaskIds(taskIds);
        DataCollect dataCollect = internalTaskService.getTaskDataCollectByIds(taskEntities, taskStaffMapEntities);
        Map<Long, List<TaskOutput>> taskOutputMap = new HashMap<>();
        for (ProjectProgressTaskMapEntity progressTaskMapEntity : progressTaskMapEntities) {
            taskOutputMap.computeIfAbsent(progressTaskMapEntity.getProjectProgressId(), k -> new ArrayList<>())
                    .add(buildTaskOutput(getData(dataCollect.getTaskEntityMap(), progressTaskMapEntity.getTaskId()), dataCollect));
        }
        return taskOutputMap;
    }

    private TaskOutput buildTaskOutput(TaskEntity taskEntity,
                                       DataCollect dataCollect) {
        TaskOutput taskOutput = taskMapper.toOutput(taskEntity);
        taskOutput.setReviewer(getReviewerDto(getData(dataCollect.getStaffEntityMap(), taskEntity.getReviewerId())));
        taskOutput.setStaffs(getData(dataCollect.getStaffSimpleListMap(), taskEntity.getId()));
        return taskOutput;
    }

    private ReviewerDto getReviewerDto(StaffEntity staffEntity) {
        if (staffEntity == null) return null;
        return ReviewerDto.builder()
                .id(staffEntity.getId())
                .name(staffEntity.getName())
                .email(staffEntity.getEmail())
                .avatar(staffEntity.getAvatar())
                .firstName(staffEntity.getFirstName())
                .lastName(staffEntity.getLastName())
                .build();
    }
}
