package com.backend.constructor.core.service.internal.impl;

import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.port.repository.StaffRepository;
import com.backend.constructor.core.service.dto.DataCollect;
import com.backend.constructor.core.service.internal.InternalTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.backend.constructor.core.service.HelperService.addIfNotNull;
import static com.backend.constructor.core.service.HelperService.getData;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalTaskServiceImpl implements InternalTaskService {
    private final StaffRepository staffRepository;

    @Override
    public DataCollect getTaskDataCollectByIds(List<TaskEntity> taskEntities, List<TaskStaffMapEntity> taskStaffMapEntities) {
        Map<Long, TaskEntity> taskEntityMap = new HashMap<>();
        Set<Long> staffIds = new HashSet<>();
        for (TaskEntity taskEntity : taskEntities) {
            taskEntityMap.put(taskEntity.getId(), taskEntity);
            addIfNotNull(staffIds, taskEntity.getReviewerId());
        }

        for (TaskStaffMapEntity taskStaffMapEntity : taskStaffMapEntities) {
            addIfNotNull(staffIds, taskStaffMapEntity.getStaffId());
        }
        Map<Long, StaffEntity> staffMap = staffRepository.getMapStaffEntityByIds(staffIds);
        Map<Long, List<CodeNameResponse>> staffSimpleListMap = new HashMap<>();
        for (TaskStaffMapEntity taskStaffMapEntity : taskStaffMapEntities) {
            CodeNameResponse staffSimple = getStaffSimple(taskStaffMapEntity, staffMap);
            if (staffSimple == null) continue;
            staffSimpleListMap.computeIfAbsent(taskStaffMapEntity.getTaskId(), k -> new ArrayList<>())
                    .add(staffSimple);
        }
        return DataCollect.builder()
                .taskEntityMap(taskEntityMap)
                .staffEntityMap(staffMap)
                .staffSimpleListMap(staffSimpleListMap)
                .build();
    }

    private CodeNameResponse getStaffSimple(TaskStaffMapEntity taskStaffMapEntity,
                                            Map<Long, StaffEntity> staffMap) {
        StaffEntity staffEntity = getData(staffMap, taskStaffMapEntity.getStaffId());
        if (staffEntity == null) return null;
        return CodeNameResponse.builder()
                .id(staffEntity.getId())
                .code(staffEntity.getCode())
                .name(staffEntity.getName())
                .build();
    }
}
