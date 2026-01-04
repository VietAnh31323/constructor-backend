package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.TaskStaffMapEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskStaffMapJpaRepository extends BaseJpaRepository<TaskStaffMapEntity> {
    void deleteAllByTaskId(Long taskId);

    List<TaskStaffMapEntity> findAllByTaskId(Long taskId);

    void deleteAllByTaskIdIn(Collection<Long> taskIds);

    List<TaskStaffMapEntity> findAllByTaskIdIn(Collection<Long> taskIds);
}
