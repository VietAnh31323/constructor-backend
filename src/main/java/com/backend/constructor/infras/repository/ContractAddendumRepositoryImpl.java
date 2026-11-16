package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.ContractAddendumEntity;
import com.backend.constructor.core.port.repository.ContractAddendumRepository;
import com.backend.constructor.infras.repository.jpa.ContractAddendumJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ContractAddendumRepositoryImpl extends JpaRepositoryAdapter<ContractAddendumEntity> implements ContractAddendumRepository {
    private final ContractAddendumJpaRepository contractAddendumJpaRepository;

    @Override
    public List<ContractAddendumEntity> getListByContractId(Long contractId) {
        if (contractId == null) {
            return Collections.emptyList();
        }
        return contractAddendumJpaRepository.findAllByContractIdAndDeletedAtIsNull(contractId);
    }

    @Override
    public List<ContractAddendumEntity> getListByProjectId(Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        return contractAddendumJpaRepository.findAllByProjectIdAndDeletedAtIsNull(projectId);
    }

    @Override
    public List<ContractAddendumEntity> getListByContractIds(Collection<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }
        return contractAddendumJpaRepository.findAllByContractIdInAndDeletedAtIsNull(contractIds);
    }

    @Override
    public Optional<ContractAddendumEntity> findByContractIdAndAddendumNumber(Long contractId, String addendumNumber) {
        if (contractId == null || addendumNumber == null || addendumNumber.isBlank()) {
            return Optional.empty();
        }
        return contractAddendumJpaRepository.findByContractIdAndAddendumNumberAndDeletedAtIsNull(contractId, addendumNumber);
    }

    @Override
    public void deleteAllByProjectId(Long projectId) {
        if (projectId != null) {
            contractAddendumJpaRepository.deleteAllByProjectId(projectId);
        }
    }

    @Override
    public void deleteAllByContractId(Long contractId) {
        if (contractId != null) {
            contractAddendumJpaRepository.deleteAllByContractId(contractId);
        }
    }
}
