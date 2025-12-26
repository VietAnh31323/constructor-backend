package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.app.dto.customer.CustomerFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CustomerApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse create(CustomerDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse update(CustomerDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse delete(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    CustomerDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    Page<CustomerDto> getListStaff(CustomerFilterParam param,
                                   Pageable pageable);
}
