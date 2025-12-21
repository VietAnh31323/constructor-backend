package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.progress.ProgressDto;
import com.backend.constructor.app.dto.progress.ProgressFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProgressApi {
    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse create(ProgressDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse update(ProgressDto input);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    IdResponse delete(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    ProgressDto getDetail(Long id);

    @PreAuthorize("hasAnyAuthority('SCOPE_ADMIN')")
    Page<ProgressDto> getListStaff(ProgressFilterParam param,
                                   Pageable pageable);
}
