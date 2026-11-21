package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProjectLineJpaRepository extends BaseJpaRepository<ProjectLineEntity> {
    List<ProjectLineEntity> findAllByProjectId(Long projectId);

    @Query("SELECT l FROM ProjectLineEntity l WHERE l.paymentDate IS NOT NULL AND l.paymentDate <= :dueEnd "
            + "AND (l.status IS NULL OR l.status <> com.backend.constructor.core.domain.enums.PaymentPlanStatus.PAID) "
            + "AND (l.lastDueAlertAt IS NULL OR l.lastDueAlertAt < :since)")
    List<ProjectLineEntity> findDuePaymentsForAlert(@Param("dueEnd") LocalDate dueEnd,
                                                    @Param("since") LocalDateTime since);
}
