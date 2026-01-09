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
import com.backend.constructor.core.domain.entity.StaffEntity;
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

    @Override
    @Transactional
    public IdResponse create(TaskDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.CV, TaskEntity.class);
        input.setId(null);
        TaskEntity taskEntity = taskMapper.toEntity(input);
        taskEntity.setState(TaskState.NOT_STARTED);
        uniqueValidationService.validate(taskEntity);
        StaffEntity staffEntity = staffRepository.getStaffByUsername(HelperService.getUsernameLogin());
        taskEntity.setReviewerId(staffEntity.getId());
        taskRepository.save(taskEntity);
        saveTaskStaffMap(taskEntity.getId(), input.getTaskStaffMaps());
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
        taskMapper.update(input, taskEntity);
        taskEntity.setState(TaskState.NOT_STARTED);
        uniqueValidationService.validate(taskEntity);
        taskRepository.save(taskEntity);
        taskStaffMapRepository.deleteAllByTaskId(taskEntity.getId());
        saveTaskStaffMap(taskEntity.getId(), input.getTaskStaffMaps());
        return IdResponse.builder().id(taskEntity.getId()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TaskEntity taskEntity = taskRepository.getTaskById(id);
        if (!TaskState.NOT_STARTED.equals(taskEntity.getState())) {
            throw BusinessException.exception("CST018");
        }
        List<TaskEntity> taskEntities = taskRepository.getListTaskByParentId(taskEntity.getId());
        Set<Long> taskIds = new HashSet<>();
        addIfNotNull(taskIds, taskEntity.getId());
        for (TaskEntity task : taskEntities) {
            addIfNotNull(taskIds, task.getId());
        }
        taskStaffMapRepository.deleteAllByTaskIds(taskIds);
        taskRepository.deleteAll(taskEntities);
        taskRepository.delete(taskEntity);
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

    @Override
    @Transactional
    public void updateState(UpdateStateDto input) {
        TaskEntity task = taskRepository.getTaskById(input.getId());
        if (input.getState() == null) throw BusinessException.exception("CST019");
        task.setState(input.getState());
        taskRepository.save(task);
        if (task.getParentId() != null) {
            TaskEntity parentTask = taskRepository.getTaskById(task.getParentId());
            syncStateParentTask(parentTask);
            syncStateProjectTask(parentTask);
            return;
        }
        if (TaskState.COMPLETED.equals(task.getState())) {
            task.setProgressPercent(BigDecimal.valueOf(100));
        }
        syncStateProjectTask(task);
    }

    private void saveTaskStaffMap(Long taskId,
                                  List<TaskStaffMapDto> taskStaffMaps) {
        if (taskStaffMaps == null || taskStaffMaps.isEmpty()) return;
        List<TaskStaffMapEntity> taskStaffMapEntities = new ArrayList<>();
        for (TaskStaffMapDto taskStaffMapDto : taskStaffMaps) {
            if (taskStaffMapDto.getStaff() == null || taskStaffMapDto.getStaff().getId() == null) continue;
            TaskStaffMapEntity taskStaffMapEntity = TaskStaffMapEntity.builder()
                    .taskId(taskId)
                    .staffId(taskStaffMapDto.getStaff().getId())
                    .build();
            taskStaffMapEntities.add(taskStaffMapEntity);
        }
        taskStaffMapRepository.saveAll(taskStaffMapEntities);
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

    private void syncStateParentTask(TaskEntity parentTask) {
        if (Objects.isNull(parentTask)) return;
        List<TaskEntity> children = taskRepository.getListTaskByParentId(parentTask.getId());
        if (children.isEmpty()) return;
        TaskState newState = getTaskState(children);
        parentTask.setState(newState);
        parentTask.setProgressPercent(calculateProgress(children));

        taskRepository.save(parentTask);
    }

    private TaskState getTaskState(List<TaskEntity> taskEntities) {
        if (taskEntities == null || taskEntities.isEmpty()) {
            return TaskState.NOT_STARTED;
        }

        boolean hasNotStarted = false;
        boolean hasCompleted = false;

        for (TaskEntity child : taskEntities) {
            TaskState s = child.getState();
            if (TaskState.IN_PROGRESS.equals(s)) {
                return TaskState.IN_PROGRESS;
            }
            if (TaskState.NOT_STARTED.equals(s)) {
                hasNotStarted = true;
            } else if (TaskState.COMPLETED.equals(s)) {
                hasCompleted = true;
            }
            if (hasNotStarted && hasCompleted) {
                return TaskState.IN_PROGRESS;
            }
        }
        if (hasNotStarted) {
            return TaskState.NOT_STARTED;
        }
        return TaskState.COMPLETED;
    }

    private void syncStateProjectTask(TaskEntity parentTask) {
        ProjectEntity projectEntity = projectRepository.getProjectByTaskId(parentTask.getId());
        if (Objects.isNull(projectEntity)) return;
        List<TaskEntity> taskEntities = taskRepository.getListTaskByProjectId(projectEntity.getId());
        projectEntity.setProgressPercent(calculateProgress(taskEntities));
        projectRepository.save(projectEntity);
    }

    private BigDecimal calculateProgress(List<TaskEntity> taskEntities) {
        if (taskEntities == null || taskEntities.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long completedCount = taskEntities.stream()
                .filter(t -> TaskState.COMPLETED.equals(t.getState()))
                .count();
        double progress = (double) completedCount / taskEntities.size() * 100;
        // Sử dụng setScale để làm tròn (ví dụ 2 chữ số thập phân) nếu cần
        return BigDecimal.valueOf(progress);
    }
}
