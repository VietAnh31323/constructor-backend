package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.TaskEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public interface TaskRepository extends BaseRepository<TaskEntity> {

    TaskEntity getTaskById(Long id);

    List<TaskEntity> getListTaskByParentId(Long parentId);

    List<TaskEntity> getListTaskByParentIds(Collection<Long> parentIds);

    Map<Long, TaskEntity> getTaskMapByIds(Collection<Long> taskIds);

    List<TaskEntity> getListTaskByProjectId(Long projectId);

    List<TaskEntity> getListTaskByProjectProgressId(Long projectProgressId);

    List<TaskEntity> getListByReviewerId(Long reviewerId);

    List<TaskEntity> getListByReviewerIds(Collection<Long> reviewerIds);
}
