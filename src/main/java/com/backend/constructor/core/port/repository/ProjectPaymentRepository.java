package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProjectPaymentEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProjectPaymentRepository extends BaseRepository<ProjectPaymentEntity> {
    List<ProjectPaymentEntity> getListByProjectId(Long projectId);

    BigDecimal sumConfirmedAmountByProjectId(Long projectId);

    void deleteAllByProjectId(Long projectId);
}
