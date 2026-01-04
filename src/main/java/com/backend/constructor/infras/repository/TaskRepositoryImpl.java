package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.TaskEntity;
import com.backend.constructor.core.port.repository.TaskRepository;
import com.backend.constructor.infras.repository.jpa.TaskJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl extends JpaRepositoryAdapter<TaskEntity> implements TaskRepository {
    private final TaskJpaRepository taskJpaRepository;

    @Override
    public TaskEntity getTaskById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public List<TaskEntity> getListTaskByParentId(Long parentId) {
        return taskJpaRepository.findAllByParentId(parentId);
    }
}
