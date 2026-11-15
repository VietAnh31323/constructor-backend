package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CustomerRepository extends BaseRepository<CustomerEntity> {
    CustomerEntity getCustomerById(Long id);

    Optional<CustomerEntity> findFirstByPhone(String phone);

    Page<CustomerEntity> getPageCustomer(CustomerFilterParam param, Pageable pageable);

    Map<Long, CodeNameResponse> getMapSimpleCustomerByIds(Collection<Long> ids);

    Map<Long, CustomerEntity> getMapCustomerEntityByIds(Collection<Long> ids);
}
