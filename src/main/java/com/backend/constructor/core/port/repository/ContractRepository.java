package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ContractEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends BaseRepository<ContractEntity> {
    List<ContractEntity> getListByProjectId(Long projectId);

    List<ContractEntity> getListByProjectIds(Collection<Long> projectIds);

    Optional<ContractEntity> findByContractNumber(String contractNumber);

    Optional<ContractEntity> findMainContractByProjectId(Long projectId);

    void deleteAllByProjectId(Long projectId);
}
