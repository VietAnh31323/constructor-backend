package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ContractAddendumEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractAddendumRepository extends BaseRepository<ContractAddendumEntity> {
    List<ContractAddendumEntity> getListByContractId(Long contractId);

    List<ContractAddendumEntity> getListByProjectId(Long projectId);

    List<ContractAddendumEntity> getListByContractIds(Collection<Long> contractIds);

    Optional<ContractAddendumEntity> findByContractIdAndAddendumNumber(Long contractId, String addendumNumber);

    void deleteAllByProjectId(Long projectId);

    void deleteAllByContractId(Long contractId);
}
