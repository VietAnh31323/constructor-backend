package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ContractAddendumEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractAddendumJpaRepository extends BaseJpaRepository<ContractAddendumEntity> {
    List<ContractAddendumEntity> findAllByContractIdAndDeletedAtIsNull(Long contractId);

    List<ContractAddendumEntity> findAllByProjectIdAndDeletedAtIsNull(Long projectId);

    List<ContractAddendumEntity> findAllByContractIdInAndDeletedAtIsNull(Collection<Long> contractIds);

    Optional<ContractAddendumEntity> findByContractIdAndAddendumNumberAndDeletedAtIsNull(Long contractId, String addendumNumber);

    void deleteAllByProjectId(Long projectId);

    void deleteAllByContractId(Long contractId);
}
