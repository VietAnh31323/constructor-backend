package com.backend.constructor.core.service;

import com.backend.constructor.app.api.CustomerApi;
import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import com.backend.constructor.common.service.GenerateCodeService;
import com.backend.constructor.common.validator.UniqueValidationService;
import com.backend.constructor.core.domain.constant.Constants;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import com.backend.constructor.core.domain.enums.ContactStatus;
import com.backend.constructor.core.port.mapper.CustomerMapper;
import com.backend.constructor.core.port.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService implements CustomerApi {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final GenerateCodeService generateCodeService;
    private final UniqueValidationService uniqueValidationService;

    @Override
    @Transactional
    public IdResponse create(CustomerDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.KH, CustomerEntity.class);
        input.setId(null);
        CustomerEntity customerEntity = customerMapper.toEntity(input);
        if (Objects.isNull(customerEntity.getContactStatus()))
            customerEntity.setContactStatus(ContactStatus.NOT_CONTACTED);
        uniqueValidationService.validate(customerEntity);
        customerRepository.save(customerEntity);
        return IdResponse.builder().id(customerEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse update(CustomerDto input) {
        input.trimData();
        generateCodeService.generateCode(input, Constants.KH, CustomerEntity.class);
        CustomerEntity customerEntity = customerRepository.getCustomerById(input.getId());
        customerMapper.update(input, customerEntity);
        if (Objects.isNull(customerEntity.getContactStatus()))
            customerEntity.setContactStatus(ContactStatus.NOT_CONTACTED);
        uniqueValidationService.validate(customerEntity);
        customerRepository.save(customerEntity);
        return IdResponse.builder().id(customerEntity.getId()).build();
    }

    @Override
    @Transactional
    public IdResponse delete(Long id) {
        CustomerEntity customerEntity = customerRepository.getCustomerById(id);
        customerRepository.delete(customerEntity);
        return IdResponse.builder().id(customerEntity.getId()).build();
    }

    @Override
    public CustomerDto getDetail(Long id) {
        CustomerEntity customerEntity = customerRepository.getCustomerById(id);
        return customerMapper.toDto(customerEntity);
    }

    @Override
    public Page<CustomerDto> getListStaff(CustomerFilterParam param, Pageable pageable) {
        Page<CustomerEntity> customerEntities = customerRepository.getPageCustomer(param, pageable);
        return customerEntities.map(customerMapper::toDto);
    }
}
