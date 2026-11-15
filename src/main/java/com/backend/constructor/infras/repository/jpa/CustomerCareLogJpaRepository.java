package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.CustomerCareLogEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerCareLogJpaRepository extends BaseJpaRepository<CustomerCareLogEntity> {
    List<CustomerCareLogEntity> findByCustomerIdOrderByCareTimeDescIdDesc(Long customerId);

    List<CustomerCareLogEntity> findByCustomerIdInOrderByCareTimeDescIdDesc(Collection<Long> customerIds);
}
