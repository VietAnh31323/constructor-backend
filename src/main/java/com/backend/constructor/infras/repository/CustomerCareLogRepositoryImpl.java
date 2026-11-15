package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.CustomerCareLogEntity;
import com.backend.constructor.core.port.repository.CustomerCareLogRepository;
import com.backend.constructor.infras.repository.jpa.CustomerCareLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomerCareLogRepositoryImpl extends JpaRepositoryAdapter<CustomerCareLogEntity> implements CustomerCareLogRepository {
    private final CustomerCareLogJpaRepository customerCareLogJpaRepository;

    @Override
    public CustomerCareLogEntity getCareLogById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST038"));
    }

    @Override
    public List<CustomerCareLogEntity> getListByCustomerId(Long customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        return customerCareLogJpaRepository.findByCustomerIdOrderByCareTimeDescIdDesc(customerId);
    }

    @Override
    public List<CustomerCareLogEntity> getListByCustomerIds(Collection<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return customerCareLogJpaRepository.findByCustomerIdInOrderByCareTimeDescIdDesc(customerIds);
    }
}
