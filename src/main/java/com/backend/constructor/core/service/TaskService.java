package com.backend.constructor.core.service;

import com.backend.constructor.app.api.TaskApi;
import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.app.dto.task.TaskStaffMapDto;
import com.backend.constructor.app.dto.task.UpdateStateDto;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.domain.enums.TaskState;
import com.backend.constructor.core.port.mapper.TaskMapper;
import com.backend.constructor.core.port.repository.ProjectRepository;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.core.port.repository.TaskRepository;
import com.backend.constructor.core.port.repository.TaskStaffMapRepository;
import com.backend.constructor.core.service.dto.DataCollect;
import com.backend.constructor.core.service.internal.InternalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.backend.constructor.core.service.HelperService.addIfNotNull;
import static com.backend.constructor.core.service.HelperService.getData;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService implements TaskApi {
    private final GenerateCodeService generateCodeService;
    private final TaskMapper taskMapper;
    private final UniqueValidationService uniqueValidationService;
    private final TaskRepository taskRepository;
    private final TaskStaffMapRepository taskStaffMapRepository;
    private final StaffRepository staffRepository;
    private final InternalTaskService internalTaskService;
    private final ProjectRepository projectRepository;
    private final ProgressCalculationService progressCalculationService;
    private final TaskWorkflowService taskWorkflowService;
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public IdResponse create(TaskDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.CV, TaskEntity.class);
        input.setId(null);
        TaskEntity taskEntity = taskMapper.toEntity(input);
        taskEntity.setState(TaskState.NOT_STARTED);
        // 2.22: % tiến độ do hệ thống tính – không nhận giá trị nhập tay
        taskEntity.setProgressPercent(BigDecimal.ZERO);
        taskEntity.setWeight(ProgressCalculationService.weightOf(input.getWeight()));
        clearSystemFields(taskEntity);
        uniqueValidationService.validate(taskEntity);
        if (input.getReviewer() != null && input.getReviewer().getId() != null) {
            taskEntity.setReviewerId(input.getReviewer().getId());
        } else {
            staffRepository.findOptionalStaffByUsername(HelperService.getUsernameLogin())
                    .ifPresent(staffEntity -> taskEntity.setReviewerId(staffEntity.getId()));
        }
        taskRepository.save(taskEntity);
        saveTaskStaffMap(taskEntity.getId(), input.getTaskStaffMaps());
        progressCalculationService.recalculateFromTask(taskEntity.getId());
        return IdResponse.builder().id(taskEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(TaskDto input) {
        TaskEntity taskEntity = taskRepository.getTaskById(input.getId());
        if (!TaskState.NOT_STARTED.equals(taskEntity.getState())) {
            throw BusinessException.exception("CST018");
        }
        input.trimData();
        generateCodeService.generateCode(input, Constants.CV, TaskEntity.class);

        // Giữ nguyên các trường do hệ thống quản lý
        BigDecimal oldPct = taskEntity.getProgressPercent();
        LocalDate oldActualStart = taskEntity.getActualStartDate();
        LocalDate oldActualEnd = taskEntity.getActualEndDate();
        LocalDateTime oldSubmitted = taskEntity.getSubmittedAt();
        LocalDateTime oldReviewed = taskEntity.getReviewedAt();
        Long oldReviewedBy = taskEntity.getReviewedById();
        String oldReturnReason = taskEntity.getReturnReason();
        LocalDateTime oldNotified = taskEntity.getOverdueNotifiedAt();

        taskMapper.update(input, taskEntity);
        taskEntity.setState(TaskState.NOT_STARTED);
        taskEntity.setProgressPercent(oldPct);
        taskEntity.setActualStartDate(oldActualStart);
        taskEntity.setActualEndDate(oldActualEnd);
        taskEntity.setSubmittedAt(oldSubmitted);
        taskEntity.setReviewedAt(oldReviewed);
        taskEntity.setReviewedById(oldReviewedBy);
        taskEntity.setReturnReason(oldReturnReason);
        taskEntity.setOverdueNotifiedAt(oldNotified);
        taskEntity.setWeight(ProgressCalculationService.weightOf(input.getWeight()));
        if (input.getReviewer() != null && input.getReviewer().getId() != null) {
            taskEntity.setReviewerId(input.getReviewer().getId());
        }

        uniqueValidationService.validate(taskEntity);
        taskRepository.save(taskEntity);
        taskStaffMapRepository.deleteAllByTaskId(taskEntity.getId());
        saveTaskStaffMap(taskEntity.getId(), input.getTaskStaffMaps());
        progressCalculationService.recalculateFromTask(taskEntity.getId());
        return IdResponse.builder().id(taskEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TaskEntity taskEntity = taskRepository.getTaskById(id);
        if (!TaskState.NOT_STARTED.equals(taskEntity.getState())) {
            throw BusinessException.exception("CST018");
        }
        Long projectId = progressCalculationService.resolveProjectIdOfTask(id);
        List<TaskEntity> taskEntities = taskRepository.getListTaskByParentId(taskEntity.getId());
        Set<Long> taskIds = new HashSet<>();
        addIfNotNull(taskIds, taskEntity.getId());
        for (TaskEntity task : taskEntities) {
            addIfNotNull(taskIds, task.getId());
        }
        taskStaffMapRepository.deleteAllByTaskIds(taskIds);
        taskRepository.deleteAll(taskEntities);
        taskRepository.delete(taskEntity);
        if (projectId != null) {
            progressCalculationService.recalculateProject(projectId);
        }
    }

    @Override
    public TaskDto getDetail(Long id) {
        TaskEntity taskEntity = taskRepository.getTaskById(id);
        List<TaskStaffMapEntity> taskStaffMapEntities = taskStaffMapRepository.getListByTaskId(taskEntity.getId());
        Set<Long> staffIds = new HashSet<>();
        addIfNotNull(staffIds, taskEntity.getReviewerId());
        for (TaskStaffMapEntity taskStaffMapEntity : taskStaffMapEntities) {
            addIfNotNull(staffIds, taskStaffMapEntity.getStaffId());
        }
        Map<Long, CodeNameResponse> staffMap = staffRepository.getMapSimpleStaffByIds(staffIds);
        TaskDto taskDto = taskMapper.toDto(taskEntity);
        taskDto.setReviewer(getData(staffMap, taskEntity.getReviewerId()));
        taskDto.setTaskStaffMaps(getTaskStaffMaps(taskStaffMapEntities, staffMap));

        // 2.23: Các bước chuyển hợp lệ với người dùng hiện tại
        boolean hasChildren = !taskRepository.getListTaskByParentId(taskEntity.getId()).isEmpty();
        Long projectId = progressCalculationService.resolveProjectIdOfTask(taskEntity.getId());
        Long pmId = projectId != null
                ? projectRepository.findById(projectId).map(ProjectEntity::getManagerId).orElse(null)
                : null;
        Set<Long> assigneeIds = taskStaffMapEntities.stream()
                .map(TaskStaffMapEntity::getStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        TaskWorkflowService.Actor actor = taskWorkflowService.currentActor();
        taskDto.setHasChildren(hasChildren);
        taskDto.setCanReview(taskWorkflowService.isReviewer(taskEntity, pmId, actor));
        taskDto.setAllowedNextStates(taskWorkflowService.allowedNextStates(taskEntity, hasChildren, assigneeIds, pmId, actor));
        return taskDto;
    }

    @Override
    public List<TaskOutput> getListTaskByProgress(Long projectProgressId) {
        List<TaskEntity> taskEntities = taskRepository.getListTaskByProjectProgressId(projectProgressId);
        List<TaskStaffMapEntity> taskStaffMapEntities = taskStaffMapRepository.getListByTaskIds(taskEntities.stream().map(TaskEntity::getId).collect(Collectors.toSet()));
        DataCollect dataCollect = internalTaskService.getTaskDataCollectByIds(taskEntities, taskStaffMapEntities);
        return taskEntities.stream().map(entity -> buildTaskOutput(entity, dataCollect)).toList();
    }

    @Override
    public List<TaskOutput> getListTaskSub(Long parentId) {
        List<TaskEntity> taskEntities = taskRepository.getListTaskByParentId(parentId);
        List<TaskStaffMapEntity> taskStaffMapEntities = taskStaffMapRepository.getListByTaskIds(taskEntities.stream().map(TaskEntity::getId).collect(Collectors.toSet()));
        DataCollect dataCollect = internalTaskService.getTaskDataCollectByIds(taskEntities, taskStaffMapEntities);
        return taskEntities.stream().map(entity -> buildTaskOutput(entity, dataCollect)).toList();
    }

    /**
     * 2.23: Chuyển trạng thái theo workflow (Chưa làm → Đang làm → Chờ duyệt → Hoàn thành / Trả lại).
     * % tiến độ việc cha / giai đoạn / dự án được tự tính lại (2.22).
     */
    @Override
    @Transactional
    public void updateState(UpdateStateDto input) {
        taskWorkflowService.transition(input.getId(), input.getState(), input.getReason());
    }

    private void clearSystemFields(TaskEntity entity) {
        entity.setActualStartDate(null);
        entity.setActualEndDate(null);
        entity.setSubmittedAt(null);
        entity.setReviewedAt(null);
        entity.setReviewedById(null);
        entity.setReturnReason(null);
        entity.setOverdueNotifiedAt(null);
    }

    private void saveTaskStaffMap(Long taskId,
                                  List<TaskStaffMapDto> taskStaffMaps) {
        if (taskStaffMaps == null || taskStaffMaps.isEmpty()) return;
        List<TaskStaffMapEntity> taskStaffMapEntities = new ArrayList<>();
        Set<Long> assignedStaffIds = new LinkedHashSet<>();
        for (TaskStaffMapDto taskStaffMapDto : taskStaffMaps) {
            if (taskStaffMapDto.getStaff() == null || taskStaffMapDto.getStaff().getId() == null) continue;
            TaskStaffMapEntity taskStaffMapEntity = TaskStaffMapEntity.builder()
                    .taskId(taskId)
                    .staffId(taskStaffMapDto.getStaff().getId())
                    .build();
            taskStaffMapEntities.add(taskStaffMapEntity);
            assignedStaffIds.add(taskStaffMapDto.getStaff().getId());
        }
        taskStaffMapRepository.saveAll(taskStaffMapEntities);
        if (!assignedStaffIds.isEmpty()) {
            taskRepository.findById(taskId).ifPresent(task -> {
                Long projectId = progressCalculationService.resolveProjectIdOfTask(taskId);
                notificationService.notifyStaffs(
                        assignedStaffIds,
                        com.backend.constructor.core.domain.enums.NotificationType.TASK_ASSIGNED,
                        "Bạn được giao công việc: " + task.getName(),
                        "Mã công việc " + task.getCode()
                                + (task.getEndDate() != null ? " — Hạn hoàn thành: " + task.getEndDate() : ""),
                        TaskWorkflowService.buildTaskLink(projectId, taskId),
                        "TASK",
                        taskId,
                        currentUserService.getCurrentStaffId()
                );
            });
        }
    }

    private List<TaskStaffMapDto> getTaskStaffMaps(List<TaskStaffMapEntity> taskStaffMapEntities,
                                                   Map<Long, CodeNameResponse> staffMap) {
        if (taskStaffMapEntities == null || taskStaffMapEntities.isEmpty()) return Collections.emptyList();
        List<TaskStaffMapDto> taskStaffMaps = new ArrayList<>();
        for (TaskStaffMapEntity taskStaffMapEntity : taskStaffMapEntities) {
            taskStaffMaps.add(TaskStaffMapDto.builder()
                    .id(taskStaffMapEntity.getId())
                    .staff(getData(staffMap, taskStaffMapEntity.getStaffId()))
                    .build());
        }
        return taskStaffMaps;
    }

    private TaskOutput buildTaskOutput(TaskEntity taskEntity,
                                       DataCollect dataCollect) {
        TaskOutput taskOutput = taskMapper.toOutput(taskEntity);
        taskOutput.setStaffs(getData(dataCollect.getStaffSimpleListMap(), taskEntity.getId()));
        return taskOutput;
    }
}
