package com.backend.constructor.core.service;

import com.backend.constructor.app.api.TaskApi;
import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.app.dto.task.TaskStaffMapDto;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.domain.enums.TaskState;
import com.backend.constructor.core.port.mapper.TaskMapper;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.core.port.repository.TaskRepository;
import com.backend.constructor.core.port.repository.TaskStaffMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
}
