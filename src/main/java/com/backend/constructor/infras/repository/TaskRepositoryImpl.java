package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterJoiner;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.repository.TaskRepository;
import com.backend.constructor.infras.repository.jpa.TaskJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl extends JpaRepositoryAdapter<TaskEntity> implements TaskRepository {
    private final EntityManager entityManager;
    private final TaskJpaRepository taskJpaRepository;

    @Override
    public TaskEntity getTaskById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public List<TaskEntity> getListTaskByParentId(Long parentId) {
        return taskJpaRepository.findAllByParentId(parentId);
    }

    @Override
    public Map<Long, TaskEntity> getTaskMapByIds(Collection<Long> taskIds) {
        return findAllByIds(taskIds).stream()
                .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));
    }

    @Override
    public List<TaskEntity> getListTaskByProjectId(Long projectId) {
        Filter<TaskEntity> filter = Filter.builder()
                .leftJoin(TaskEntity_.ID, new FilterJoiner(ProjectProgressTaskMapEntity.class, ProjectProgressTaskMapEntity_.TASK_ID))
                .thenLeftJoin(ProjectProgressTaskMapEntity_.PROJECT_PROGRESS_ID, new FilterJoiner(ProjectProgressEntity.class, ProjectProgressEntity_.ID))
                .filter()
                .isEqual(ProjectProgressEntity_.PROJECT_ID, projectId)
                .withContext(entityManager)
                .build(TaskEntity.class);
        return filter.getList();
    }
}
