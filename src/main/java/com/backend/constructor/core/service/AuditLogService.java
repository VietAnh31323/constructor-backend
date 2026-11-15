package com.backend.constructor.core.service;

import com.backend.constructor.app.api.AuditLogApi;
import com.backend.constructor.app.dto.audit_log.AuditLogDto;
import com.backend.constructor.app.dto.audit_log.AuditLogFilterParam;
import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.domain.entity.*;
import com.backend.constructor.core.port.repository.AuditLogRepository;
import com.backend.constructor.infras.repository.jpa.AuditLogJpaRepository;
import com.backend.constructor.user.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService implements AuditLogApi {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "otpHash", "genPassword", "token", "refreshToken", "tokensValidAfter");

    private static final Set<String> IGNORED_META_FIELDS = Set.of(
            "createdAt", "updatedAt", "createdBy", "updatedBy", "serialVersionUID");

    private static final Set<Class<?>> SKIPPED_ENTITIES = Set.of(
            AuditLogEntity.class,
            TokenEntity.class,
            PasswordResetEntity.class,
            PermissionEntity.class,
            RolePermissionMapEntity.class,
            RoleEntity.class,
            AccountRoleMapEntity.class,
            ProjectStateLogEntity.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogJpaRepository auditLogJpaRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    public Page<AuditLogDto> getListAuditLog(AuditLogFilterParam param, Pageable pageable) {
        return auditLogRepository.getPageAuditLog(param, pageable).map(this::toDto);
    }

    @Override
    public AuditLogDto getDetail(Long id) {
        return toDto(auditLogRepository.getAuditLogById(id));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExplicit(String action,
            String module,
            String entityName,
            String entityId,
            Object oldValue,
            Object newValue,
            String description,
            String actorOverride) {
        try {
            String actor = actorOverride != null
                    ? actorOverride
                    : SecurityUtils.getCurrentUserLogin().orElse("SYSTEM");
            HttpServletRequest req = currentRequest();
            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .username(actor)
                    .action(action)
                    .module(module)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(oldValue != null ? toJson(oldValue) : null)
                    .newValue(newValue != null ? toJson(newValue) : null)
                    .ipAddress(extractIp(req))
                    .userAgent(req != null ? req.getHeader("User-Agent") : null)
                    .description(description)
                    .build();
            auditLogJpaRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Failed to record explicit audit log: {}", e.getMessage());
        }
    }

    public boolean shouldSkipEntity(Object entity) {
        if (entity == null) {
            return true;
        }
        if (RequestContextHolder.getRequestAttributes() == null) {
            return true;
        }
        for (Class<?> skipped : SKIPPED_ENTITIES) {
            if (skipped.isAssignableFrom(entity.getClass())) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Object> extractLoadedState(BaseEntity entity) {
        if (shouldSkipEntity(entity) || entity.getId() == null) {
            return null;
        }
        try {
            SessionImplementor session = entityManager.unwrap(SessionImplementor.class);
            EntityEntry entry = session.getPersistenceContext().getEntry(entity);
            if (entry != null && entry.getLoadedState() != null) {
                String[] propertyNames = entry.getPersister().getPropertyNames();
                Object[] loadedState = entry.getLoadedState();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", entity.getId());
                for (int i = 0; i < propertyNames.length; i++) {
                    String prop = propertyNames[i];
                    if (IGNORED_META_FIELDS.contains(prop)) {
                        continue;
                    }
                    Object val = loadedState[i];
                    if (SENSITIVE_FIELDS.contains(prop)) {
                        map.put(prop, val != null ? "***" : null);
                    } else {
                        map.put(prop, normalizeValue(val));
                    }
                }
                return map;
            }
        } catch (Exception e) {
            log.debug("Could not read Hibernate loadedState for {}: {}", entity.getClass().getSimpleName(),
                    e.getMessage());
        }
        return extractCurrentState(entity);
    }

    public Map<String, Object> extractCurrentState(BaseEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                String name = field.getName();
                if (IGNORED_META_FIELDS.contains(name)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object val = field.get(entity);
                    if (SENSITIVE_FIELDS.contains(name)) {
                        map.put(name, val != null ? "***" : null);
                    } else {
                        map.put(name, normalizeValue(val));
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return map;
    }

    @Transactional
    public void recordEntitySave(BaseEntity saved, boolean isNew, Map<String, Object> oldState) {
        if (shouldSkipEntity(saved)) {
            return;
        }
        try {
            Map<String, Object> newState = extractCurrentState(saved);
            if (!isNew && oldState != null && Objects.equals(oldState, newState)) {
                return;
            }
            String action = isNew ? "CREATE" : "UPDATE";
            String entityName = saved.getClass().getSimpleName();
            String module = resolveModule(saved.getClass());
            String entityId = saved.getId() != null ? String.valueOf(saved.getId()) : null;
            String username = SecurityUtils.getCurrentUserLogin().orElse("SYSTEM");
            HttpServletRequest req = currentRequest();

            String description = (isNew ? "Tạo mới " : "Cập nhật ")
                    + resolveFriendlyEntityLabel(saved.getClass())
                    + (entityId != null ? " #" + entityId : "");

            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .username(username)
                    .action(action)
                    .module(module)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(isNew ? null : toJson(oldState))
                    .newValue(toJson(newState))
                    .ipAddress(extractIp(req))
                    .userAgent(req != null ? req.getHeader("User-Agent") : null)
                    .description(description)
                    .build();
            auditLogJpaRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Failed to record entity save audit log: {}", e.getMessage());
        }
    }

    @Transactional
    public void recordEntityDelete(BaseEntity deleted, Map<String, Object> oldState) {
        if (shouldSkipEntity(deleted)) {
            return;
        }
        try {
            Map<String, Object> newState = Map.of(
                    "id", deleted.getId() != null ? deleted.getId() : "",
                    "deletedAt", deleted.getDeletedAt() != null ? deleted.getDeletedAt().toString() : "",
                    "deletedBy", deleted.getDeletedBy() != null ? deleted.getDeletedBy() : "");
            String entityName = deleted.getClass().getSimpleName();
            String module = resolveModule(deleted.getClass());
            String entityId = deleted.getId() != null ? String.valueOf(deleted.getId()) : null;
            String username = SecurityUtils.getCurrentUserLogin().orElse("SYSTEM");
            HttpServletRequest req = currentRequest();

            String description = "Xóa " + resolveFriendlyEntityLabel(deleted.getClass())
                    + (entityId != null ? " #" + entityId : "");

            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .username(username)
                    .action("DELETE")
                    .module(module)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(oldState != null ? toJson(oldState) : null)
                    .newValue(toJson(newState))
                    .ipAddress(extractIp(req))
                    .userAgent(req != null ? req.getHeader("User-Agent") : null)
                    .description(description)
                    .build();
            auditLogJpaRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("Failed to record entity delete audit log: {}", e.getMessage());
        }
    }

    private Object normalizeValue(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Enum<?> e) {
            return e.name();
        }
        if (val instanceof Number || val instanceof Boolean || val instanceof String) {
            return val;
        }
        if (val instanceof java.time.temporal.Temporal || val instanceof Date) {
            return val.toString();
        }
        if (val instanceof BaseEntity be) {
            return be.getId();
        }
        return val;
    }

    private String resolveModule(Class<?> clazz) {
        String name = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        if (name.contains("steel") || name.contains("assembly"))
            return "STEEL";
        if (name.contains("projectprogress") || name.contains("progress"))
            return "PROGRESS";
        if (name.contains("task"))
            return "TASK";
        if (name.contains("project"))
            return "PROJECT";
        if (name.contains("category"))
            return "CATEGORY";
        if (name.contains("customer"))
            return "CUSTOMER";
        if (name.contains("staff"))
            return "STAFF";
        if (name.contains("account"))
            return "ACCOUNT";
        return "SYSTEM";
    }

    private String resolveFriendlyEntityLabel(Class<?> clazz) {
        return switch (clazz.getSimpleName()) {
            case "ProjectEntity" -> "dự án";
            case "CategoryEntity" -> "hạng mục";
            case "CustomerEntity" -> "khách hàng";
            case "CustomerCareLogEntity" -> "lịch sử chăm sóc khách hàng";
            case "StaffEntity" -> "nhân sự";
            case "StaffCertificateEntity" -> "chứng chỉ hành nghề";
            case "AccountEntity" -> "tài khoản";
            case "ProgressEntity" -> "tiến trình";
            case "ProjectProgressEntity" -> "tiến độ dự án";
            case "TaskEntity" -> "công việc";
            case "SteelProjectEntity" -> "dự án thép";
            case "AssemblyEntity" -> "cấu kiện thép";
            case "SteelCategoryEntity" -> "danh mục thép";
            case "SteelEntity" -> "thanh thép";
            default -> clazz.getSimpleName();
        };
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    public static String extractIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private AuditLogDto toDto(AuditLogEntity entity) {
        return AuditLogDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .action(entity.getAction())
                .module(entity.getModule())
                .entityName(entity.getEntityName())
                .entityId(entity.getEntityId())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
