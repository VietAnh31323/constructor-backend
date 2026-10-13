package com.backend.constructor.common.base.repository;

import com.backend.constructor.common.base.entity.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public abstract class JpaRepositoryAdapter<T extends BaseEntity> implements BaseRepository<T> {

    private BaseJpaRepository<T> jpaRepository;

    protected JpaRepositoryAdapter() {
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public final void setJpaRepository(BaseJpaRepository<T> jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<T> findAll() {
        return this.jpaRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<T> findAll(@NonNull Sort sort) {
        return this.jpaRepository.findAll(sort);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Page<T> findAll(@NonNull Pageable pageable) {
        return this.jpaRepository.findAll(pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<T> findAllByIds(@NonNull Iterable<Long> ids) {
        return this.jpaRepository.findAllById(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() {
        return this.jpaRepository.count();
    }

    /**
     * {@inheritDoc}
     * Performs soft delete operation
     */
    @Override
    public void delete(@NonNull T entity) {
        entity.setDeletedAt(java.time.Instant.now());
        entity.setDeletedBy(com.backend.constructor.user.security.SecurityUtils.getCurrentUserLogin().orElse("system"));
        this.jpaRepository.save(entity);
    }

    /**
     * {@inheritDoc}
     * Performs soft delete operation on multiple entities
     */
    @Override
    public void deleteAll(@NonNull Iterable<? extends T> entities) {
        java.time.Instant now = java.time.Instant.now();
        String username = com.backend.constructor.user.security.SecurityUtils.getCurrentUserLogin().orElse("system");
        for (T entity : entities) {
            entity.setDeletedAt(now);
            entity.setDeletedBy(username);
        }
        this.jpaRepository.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     * Performs soft delete operation on all entities
     */
    @Override
    public void deleteAll() {
        List<T> all = this.jpaRepository.findAll();
        deleteAll(all);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public <S extends T> S save(@NonNull S entity) {
        return this.jpaRepository.save(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public <S extends T> List<S> saveAll(@NonNull Iterable<S> entities) {
        return this.jpaRepository.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Optional<T> findById(@NonNull Long id) {
        return this.jpaRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        this.jpaRepository.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public <S extends T> S saveAndFlush(@NonNull S entity) {
        return this.jpaRepository.saveAndFlush(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public <S extends T> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) {
        return this.jpaRepository.saveAllAndFlush(entities);
    }
}