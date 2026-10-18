package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.app.dto.task.UpdateStateDto;
import com.backend.constructor.common.base.dto.response.IdResponse;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface TaskApi {
    @PreAuthorize("hasAnyAuthority('TASK_CREATE', 'SCOPE_ADMIN')")
    IdResponse create(TaskDto input);

    @PreAuthorize("hasAnyAuthority('TASK_UPDATE', 'SCOPE_ADMIN')")
    IdResponse update(TaskDto input);

    @PreAuthorize("hasAnyAuthority('TASK_DELETE', 'SCOPE_ADMIN')")
    void delete(Long id);

    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'SCOPE_ADMIN')")
    TaskDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'SCOPE_ADMIN')")
    List<TaskOutput> getListTaskByProgress(Long projectProgressId);

    @PreAuthorize("hasAnyAuthority('TASK_VIEW', 'SCOPE_ADMIN')")
    List<TaskOutput> getListTaskSub(Long parentId);

    @PreAuthorize("hasAnyAuthority('TASK_UPDATE', 'SCOPE_ADMIN')")
    void updateState(UpdateStateDto input);
}
