package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.TaskSiteReportEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskSiteReportJpaRepository extends BaseJpaRepository<TaskSiteReportEntity> {
    List<TaskSiteReportEntity> findAllByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<TaskSiteReportEntity> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("SELECT r.taskId, COUNT(r) FROM TaskSiteReportEntity r WHERE r.taskId IN :taskIds GROUP BY r.taskId")
    List<Object[]> countGroupByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
