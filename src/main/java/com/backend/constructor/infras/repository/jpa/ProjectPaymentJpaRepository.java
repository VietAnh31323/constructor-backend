package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectPaymentEntity;
import com.backend.constructor.core.domain.enums.ProjectPaymentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProjectPaymentJpaRepository extends BaseJpaRepository<ProjectPaymentEntity> {
    List<ProjectPaymentEntity> findAllByProjectIdOrderByPaymentDateDescIdDesc(Long projectId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ProjectPaymentEntity p WHERE p.projectId = :projectId AND p.status = :status")
    BigDecimal sumAmountByProjectIdAndStatus(@Param("projectId") Long projectId,
                                             @Param("status") ProjectPaymentStatus status);

    void deleteAllByProjectId(Long projectId);
}
