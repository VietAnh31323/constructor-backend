package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.TaskCommentEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskCommentJpaRepository extends BaseJpaRepository<TaskCommentEntity> {
    List<TaskCommentEntity> findAllByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    @Query("SELECT c.taskId, COUNT(c) FROM TaskCommentEntity c WHERE c.taskId IN :taskIds GROUP BY c.taskId")
    List<Object[]> countGroupByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
