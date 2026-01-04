package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import com.backend.constructor.core.port.repository.TaskStaffMapRepository;
import com.backend.constructor.infras.repository.jpa.TaskStaffMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TaskStaffMapRepositoryImpl extends JpaRepositoryAdapter<TaskStaffMapEntity> implements TaskStaffMapRepository {
    private final TaskStaffMapJpaRepository taskStaffMapJpaRepository;

    @Override
    public void deleteAllByTaskId(Long taskId) {
        taskStaffMapJpaRepository.deleteAllByTaskId(taskId);
    }

    @Override
    public List<TaskStaffMapEntity> getListByTaskId(Long taskId) {
        return taskStaffMapJpaRepository.findAllByTaskId(taskId);
    }

    @Override
    public void deleteAllByTaskIds(Collection<Long> taskIds) {
        taskStaffMapJpaRepository.deleteAllByTaskIdIn(taskIds);
    }
}
