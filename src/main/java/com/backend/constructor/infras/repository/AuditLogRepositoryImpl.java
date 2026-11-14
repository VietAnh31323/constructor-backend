package com.backend.constructor.infras.repository;

import com.backend.constructor.app.dto.audit_log.AuditLogFilterParam;
import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.base.repository.filter.Filter;
import com.backend.constructor.common.base.repository.filter.FilterFlag;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.AuditLogEntity;
import com.backend.constructor.core.domain.entity.AuditLogEntity_;
import com.backend.constructor.core.port.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl extends JpaRepositoryAdapter<AuditLogEntity> implements AuditLogRepository {
    private final EntityManager entityManager;

    @Override
    public Page<AuditLogEntity> getPageAuditLog(AuditLogFilterParam param, Pageable pageable) {
        String search = param != null ? StringUtils.trimToNull(param.getSearch()) : null;
        String username = param != null ? StringUtils.trimToNull(param.getUsername()) : null;
        String module = param != null ? StringUtils.trimToNull(param.getModule()) : null;
        String action = param != null ? StringUtils.trimToNull(param.getAction()) : null;

        Instant fromInstant = (param != null && param.getFromDate() != null)
                ? param.getFromDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;
        Instant toInstant = (param != null && param.getToDate() != null)
                ? param.getToDate().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : null;

        Pageable sortedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Filter<AuditLogEntity> filter = Filter.builder()
                .search()
                .isContains(AuditLogEntity_.USERNAME, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(AuditLogEntity_.DESCRIPTION, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(AuditLogEntity_.ENTITY_NAME, search, FilterFlag.UNACCENT_CASE_SENSITIVE)
                .isContains(AuditLogEntity_.ENTITY_ID, search, FilterFlag.CASE_SENSITIVE)
                .filter()
                .isContains(AuditLogEntity_.USERNAME, username, FilterFlag.CASE_SENSITIVE)
                .isEqual(AuditLogEntity_.MODULE, module)
                .isEqual(AuditLogEntity_.ACTION, action)
                .isGreaterThanOrEqual("createdAt", fromInstant)
                .isLessThan("createdAt", toInstant)
                .pageable(sortedPageable)
                .withContext(entityManager)
                .build(AuditLogEntity.class);
        return filter.getPage();
    }

    @Override
    public AuditLogEntity getAuditLogById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }
}
