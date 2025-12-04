package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends BaseRepository<CustomerEntity> {
    CustomerEntity getCustomerById(Long id);

    Page<CustomerEntity> getPageCustomer(CustomerFilterParam param, Pageable pageable);
}