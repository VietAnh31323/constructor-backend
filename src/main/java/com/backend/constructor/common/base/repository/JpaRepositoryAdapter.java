package com.backend.constructor.common.base.repository;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.core.service.AuditLogService;
import com.backend.constructor.user.security.SecurityUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class JpaRepositoryAdapter<T extends BaseEntity> implements BaseRepository<T> {

    private BaseJpaRepository<T> jpaRepository;
    private ObjectProvider<AuditLogService> auditLogServiceProvider;

    protected JpaRepositoryAdapter() {
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public final void setJpaRepository(BaseJpaRepository<T> jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Autowired
    public final void setAuditLogServiceProvider(ObjectProvider<AuditLogService> auditLogServiceProvider) {
        this.auditLogServiceProvider = auditLogServiceProvider;
    }

    private AuditLogService auditLogService() {
        return auditLogServiceProvider != null ? auditLogServiceProvider.getIfAvailable() : null;
    }

    @Override
    @NonNull
    public List<T> findAll() {
        return this.jpaRepository.findAll();
    }

    @Override
    @NonNull
    public List<T> findAll(@NonNull Sort sort) {
        return this.jpaRepository.findAll(sort);
    }

    @Override
    @NonNull
    public Page<T> findAll(@NonNull Pageable pageable) {
        return this.jpaRepository.findAll(pageable);
    }

    @Override
    @NonNull
    public List<T> findAllByIds(@NonNull Iterable<Long> ids) {
        return this.jpaRepository.findAllById(ids);
    }

    @Override
    public long count() {
        return this.jpaRepository.count();
    }

    @Override
    public void delete(@NonNull T entity) {
        AuditLogService auditService = auditLogService();
        Map<String, Object> oldState = auditService != null ? auditService.extractCurrentState(entity) : null;
        entity.setDeletedAt(Instant.now());
        entity.setDeletedBy(SecurityUtils.getCurrentUserLogin().orElse("system"));
        this.jpaRepository.save(entity);
        if (auditService != null) {
            auditService.recordEntityDelete(entity, oldState);
        }
    }

    @Override
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        AuditLogService auditService = auditLogService();
        Instant now = Instant.now();
        String username = SecurityUtils.getCurrentUserLogin().orElse("system");
        List<Map<String, Object>> oldStates = new ArrayList<>();
        List<T> entityList = new ArrayList<>();
        for (T entity : entities) {
            oldStates.add(auditService != null ? auditService.extractCurrentState(entity) : null);
            entity.setDeletedAt(now);
            entity.setDeletedBy(username);
            entityList.add(entity);
        }
        this.jpaRepository.saveAll(entityList);
        if (auditService != null) {
            for (int i = 0; i < entityList.size(); i++) {
                auditService.recordEntityDelete(entityList.get(i), oldStates.get(i));
            }
        }
    }

    @Override
    public void deleteAll() {
        List<T> all = this.jpaRepository.findAll();
        deleteAll(all);
    }

    @Override
    @NonNull
    public <S extends T> S save(@NonNull S entity) {
        AuditLogService auditService = auditLogService();
        boolean isNew = entity.getId() == null;
        Map<String, Object> oldState = (!isNew && auditService != null) ? auditService.extractLoadedState(entity) : null;
        S saved = this.jpaRepository.save(entity);
        if (auditService != null) {
            auditService.recordEntitySave(saved, isNew, oldState);
        }
        return saved;
    }

    @Override
    @NonNull
    public <S extends T> List<S> saveAll(@NonNull Iterable<S> entities) {
        AuditLogService auditService = auditLogService();
        List<Boolean> isNewList = new ArrayList<>();
        List<Map<String, Object>> oldStates = new ArrayList<>();
        for (S entity : entities) {
            boolean isNew = entity.getId() == null;
            isNewList.add(isNew);
            oldStates.add((!isNew && auditService != null) ? auditService.extractLoadedState(entity) : null);
        }
        List<S> savedList = this.jpaRepository.saveAll(entities);
        if (auditService != null) {
            for (int i = 0; i < savedList.size(); i++) {
                auditService.recordEntitySave(savedList.get(i), isNewList.get(i), oldStates.get(i));
            }
        }
        return savedList;
    }

    @Override
    @NonNull
    public Optional<T> findById(@NonNull Long id) {
        return this.jpaRepository.findById(id);
    }

    @Override
    public void flush() {
        this.jpaRepository.flush();
    }

    @Override
    @NonNull
    public <S extends T> S saveAndFlush(@NonNull S entity) {
        AuditLogService auditService = auditLogService();
        boolean isNew = entity.getId() == null;
        Map<String, Object> oldState = (!isNew && auditService != null) ? auditService.extractLoadedState(entity) : null;
        S saved = this.jpaRepository.saveAndFlush(entity);
        if (auditService != null) {
            auditService.recordEntitySave(saved, isNew, oldState);
        }
        return saved;
    }

    @Override
    @NonNull
    public <S extends T> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) {
        AuditLogService auditService = auditLogService();
        List<Boolean> isNewList = new ArrayList<>();
        List<Map<String, Object>> oldStates = new ArrayList<>();
        for (S entity : entities) {
            boolean isNew = entity.getId() == null;
            isNewList.add(isNew);
            oldStates.add((!isNew && auditService != null) ? auditService.extractLoadedState(entity) : null);
        }
        List<S> savedList = this.jpaRepository.saveAllAndFlush(entities);
        if (auditService != null) {
            for (int i = 0; i < savedList.size(); i++) {
                auditService.recordEntitySave(savedList.get(i), isNewList.get(i), oldStates.get(i));
            }
        }
        return savedList;
    }
}
