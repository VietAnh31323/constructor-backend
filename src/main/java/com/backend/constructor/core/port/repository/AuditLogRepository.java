package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.audit_log.AuditLogFilterParam;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends BaseRepository<AuditLogEntity> {
    Page<AuditLogEntity> getPageAuditLog(AuditLogFilterParam param, Pageable pageable);

    AuditLogEntity getAuditLogById(Long id);
}
