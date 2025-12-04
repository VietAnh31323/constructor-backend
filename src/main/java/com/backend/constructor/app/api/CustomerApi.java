package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerApi {
    IdResponse create(CustomerDto input);

    IdResponse update(CustomerDto input);

    IdResponse delete(Long id);

    CustomerDto getDetail(Long id);

    Page<CustomerDto> getListStaff(CustomerFilterParam param,
                                   Pageable pageable);
}
