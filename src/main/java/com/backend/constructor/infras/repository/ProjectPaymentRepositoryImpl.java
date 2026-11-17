package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ProjectPaymentEntity;
import com.backend.constructor.core.domain.enums.ProjectPaymentStatus;
import com.backend.constructor.core.port.repository.ProjectPaymentRepository;
import com.backend.constructor.infras.repository.jpa.ProjectPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectPaymentRepositoryImpl extends JpaRepositoryAdapter<ProjectPaymentEntity> implements ProjectPaymentRepository {
    private final ProjectPaymentJpaRepository projectPaymentJpaRepository;

    @Override
    public List<ProjectPaymentEntity> getListByProjectId(Long projectId) {
        return projectPaymentJpaRepository.findAllByProjectIdOrderByPaymentDateDescIdDesc(projectId);
    }

    @Override
    public BigDecimal sumConfirmedAmountByProjectId(Long projectId) {
        if (projectId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = projectPaymentJpaRepository.sumAmountByProjectIdAndStatus(projectId, ProjectPaymentStatus.CONFIRMED);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        projectPaymentJpaRepository.deleteAllByProjectId(projectId);
    }
}
