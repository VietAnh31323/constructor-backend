package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.audit_log.AuditLogDto;
import com.backend.constructor.app.dto.audit_log.AuditLogFilterParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AuditLogApi {
    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_VIEW', 'SCOPE_ADMIN')")
    Page<AuditLogDto> getListAuditLog(AuditLogFilterParam param, Pageable pageable);

    @PreAuthorize("hasAnyAuthority('AUDIT_LOG_VIEW', 'SCOPE_ADMIN')")
    AuditLogDto getDetail(Long id);
}
