package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ContractEntity;
import com.backend.constructor.core.domain.enums.ContractType;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractJpaRepository extends BaseJpaRepository<ContractEntity> {
    List<ContractEntity> findAllByProjectIdAndDeletedAtIsNull(Long projectId);

    List<ContractEntity> findAllByProjectIdInAndDeletedAtIsNull(Collection<Long> projectIds);

    Optional<ContractEntity> findByContractNumberAndDeletedAtIsNull(String contractNumber);

    Optional<ContractEntity> findFirstByProjectIdAndContractTypeAndDeletedAtIsNull(Long projectId, ContractType contractType);

    void deleteAllByProjectId(Long projectId);
}
