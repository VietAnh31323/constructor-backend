package com.backend.constructor.app.rest;

import com.backend.constructor.app.api.AuditLogApi;
import com.backend.constructor.app.dto.audit_log.AuditLogDto;
import com.backend.constructor.app.dto.audit_log.AuditLogFilterParam;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit-log")
@RequiredArgsConstructor
public class AuditLogController implements AuditLogApi {
    private final AuditLogApi auditLogService;

    @Override
    @GetMapping("/list")
    @HandsomePaging
    public Page<AuditLogDto> getListAuditLog(@ParameterObject AuditLogFilterParam param,
                                             @ParameterObject Pageable pageable) {
        return auditLogService.getListAuditLog(param, pageable);
    }

    @Override
    @GetMapping
    public AuditLogDto getDetail(@RequestParam Long id) {
        return auditLogService.getDetail(id);
    }
}
