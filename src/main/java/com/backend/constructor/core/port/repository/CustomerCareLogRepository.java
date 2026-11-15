package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.CustomerCareLogEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerCareLogRepository extends BaseRepository<CustomerCareLogEntity> {
    CustomerCareLogEntity getCareLogById(Long id);

    List<CustomerCareLogEntity> getListByCustomerId(Long customerId);

    List<CustomerCareLogEntity> getListByCustomerIds(Collection<Long> customerIds);
}
