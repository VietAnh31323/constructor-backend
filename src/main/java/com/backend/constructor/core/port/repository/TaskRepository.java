package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.TaskEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends BaseRepository<TaskEntity> {

    TaskEntity getTaskById(Long id);

    List<TaskEntity> getListTaskByParentId(Long parentId);
}