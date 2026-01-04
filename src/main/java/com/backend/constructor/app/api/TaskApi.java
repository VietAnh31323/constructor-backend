package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.common.base.dto.response.IdResponse;

public interface TaskApi {
    IdResponse create(TaskDto input);

    IdResponse update(TaskDto input);

    void delete(Long id);

    TaskDto getDetail(Long id);
}
