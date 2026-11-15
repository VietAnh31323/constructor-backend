package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.customer.*;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface CustomerApi {
    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse create(CustomerDto input);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse update(CustomerDto input);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse delete(Long id);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_VIEW', 'CUSTOMER_MANAGE', 'PROJECT_VIEW', 'PROJECT_CREATE', 'PROJECT_UPDATE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    CustomerDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_VIEW', 'CUSTOMER_MANAGE', 'PROJECT_VIEW', 'PROJECT_CREATE', 'PROJECT_UPDATE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    Page<CustomerDto> getListStaff(CustomerFilterParam param,
                                   Pageable pageable);

    // --- 2.08: Pipeline trạng thái khách hàng ---
    @PreAuthorize("hasAnyAuthority('CUSTOMER_VIEW', 'CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    CustomerPipelineSummaryDto getPipelineSummary();

    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse updatePipelineStage(Long customerId, CustomerPipelineStageUpdateDto input);

    // --- 2.07: Lịch sử chăm sóc khách hàng ---
    @PreAuthorize("hasAnyAuthority('CUSTOMER_VIEW', 'CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    List<CustomerCareLogDto> getCareLogs(Long customerId);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse addCareLog(Long customerId, CustomerCareLogDto input);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse updateCareLog(Long customerId, Long logId, CustomerCareLogDto input);

    @PreAuthorize("hasAnyAuthority('CUSTOMER_MANAGE', 'SCOPE_ADMIN', 'SCOPE_CUSTOMER_CARE')")
    IdResponse deleteCareLog(Long customerId, Long logId);
}
