package com.backend.constructor.core.service.internal;

import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.service.dto.DataCollect;

import java.util.List;

public interface InternalTaskService {
    DataCollect getTaskDataCollectByIds(List<TaskEntity> taskEntities,
                                        List<TaskStaffMapEntity> taskStaffMapEntities);
}
