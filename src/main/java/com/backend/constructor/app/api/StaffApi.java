package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.staff.StaffCertificateDto;
import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.app.dto.staff.StaffFilterParam;
import com.backend.constructor.app.dto.staff.StaffOutput;
import com.backend.constructor.app.dto.staff.StaffWorkloadDto;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface StaffApi {
    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    IdResponse create(StaffDto input);

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    IdResponse update(StaffDto input);

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    IdResponse delete(Long id);

    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'SCOPE_ADMIN')")
    StaffDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'SCOPE_ADMIN')")
    Page<StaffOutput> getListStaff(StaffFilterParam param,
                                   Pageable pageable);

    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'PROJECT_VIEW', 'TASK_VIEW', 'SCOPE_ADMIN')")
    StaffWorkloadDto getStaffWorkload(Long staffId);

    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE', 'SCOPE_ADMIN')")
    List<StaffCertificateDto> getCertificates(Long staffId);

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    IdResponse addCertificate(Long staffId, StaffCertificateDto input);

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    IdResponse updateCertificate(Long staffId, Long certId, StaffCertificateDto input);

    @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'SCOPE_ADMIN')")
    IdResponse deleteCertificate(Long staffId, Long certId);
}
