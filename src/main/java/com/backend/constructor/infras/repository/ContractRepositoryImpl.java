package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ContractEntity;
import com.backend.constructor.core.domain.enums.ContractType;
import com.backend.constructor.core.port.repository.ContractRepository;
import com.backend.constructor.infras.repository.jpa.ContractJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ContractRepositoryImpl extends JpaRepositoryAdapter<ContractEntity> implements ContractRepository {
    private final ContractJpaRepository contractJpaRepository;

    @Override
    public List<ContractEntity> getListByProjectId(Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        return contractJpaRepository.findAllByProjectIdAndDeletedAtIsNull(projectId);
    }

    @Override
    public List<ContractEntity> getListByProjectIds(Collection<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return contractJpaRepository.findAllByProjectIdInAndDeletedAtIsNull(projectIds);
    }

    @Override
    public Optional<ContractEntity> findByContractNumber(String contractNumber) {
        if (contractNumber == null || contractNumber.isBlank()) {
            return Optional.empty();
        }
        return contractJpaRepository.findByContractNumberAndDeletedAtIsNull(contractNumber);
    }

    @Override
    public Optional<ContractEntity> findMainContractByProjectId(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return contractJpaRepository.findFirstByProjectIdAndContractTypeAndDeletedAtIsNull(projectId, ContractType.MAIN);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        if (projectId != null) {
            contractJpaRepository.deleteAllByProjectId(projectId);
        }
    }
}
