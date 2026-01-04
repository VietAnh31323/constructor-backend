package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskStaffMapRepository extends BaseRepository<TaskStaffMapEntity> {
    void deleteAllByTaskId(Long taskId);

    List<TaskStaffMapEntity> getListByTaskId(Long taskId);

    void deleteAllByTaskIds(Collection<Long> taskIds);
}