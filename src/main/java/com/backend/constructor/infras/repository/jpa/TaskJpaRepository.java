package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.TaskEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskJpaRepository extends BaseJpaRepository<TaskEntity> {
    List<TaskEntity> findAllByParentId(Long parentId);

    List<TaskEntity> findAllByParentIdIn(Collection<Long> parentIds);

    List<TaskEntity> findAllByReviewerId(Long reviewerId);

    List<TaskEntity> findAllByReviewerIdIn(Collection<Long> reviewerIds);

    /** 2.26: Công việc quá hạn chưa hoàn thành và chưa được cảnh báo kể từ mốc `since` */
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TaskEntity t WHERE t.endDate < :today "
            + "AND (t.state IS NULL OR t.state <> com.backend.constructor.core.domain.enums.TaskState.COMPLETED) "
            + "AND (t.overdueNotifiedAt IS NULL OR t.overdueNotifiedAt < :since)")
    List<TaskEntity> findOverdueTasksForAlert(@org.springframework.data.repository.query.Param("today") java.time.LocalDate today,
                                             @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TaskEntity t WHERE t.endDate BETWEEN :today AND :dueEnd "
            + "AND (t.state IS NULL OR t.state <> com.backend.constructor.core.domain.enums.TaskState.COMPLETED) "
            + "AND (t.overdueNotifiedAt IS NULL OR t.overdueNotifiedAt < :since)")
    List<TaskEntity> findDueSoonTasksForAlert(@org.springframework.data.repository.query.Param("today") java.time.LocalDate today,
                                              @org.springframework.data.repository.query.Param("dueEnd") java.time.LocalDate dueEnd,
                                              @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
