package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.app.dto.task.UpdateStateDto;
import com.backend.constructor.common.base.dto.response.IdResponse;

import java.util.List;

public interface TaskApi {
    IdResponse create(TaskDto input);

    IdResponse update(TaskDto input);

    void delete(Long id);

    TaskDto getDetail(Long id);

    List<TaskOutput> getListTaskSub(Long parentId);

    void updateState(UpdateStateDto input);
}
