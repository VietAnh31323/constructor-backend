package com.backend.constructor.common.base.repository;

import com.backend.constructor.common.base.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * Base Repository interface providing common CRUD operations
 * for all entities extending BaseEntity
 *
 * @param <T> Entity type extending BaseEntity
 */
public interface BaseRepository<T extends BaseEntity> {

    /**
     * Retrieves all entities
     *
     * @return List of all entities
     */
    List<T> findAll();

    /**
     * Retrieves all entities sorted by the given options
     *
     * @param sort Sort options
     * @return Sorted list of entities
     */
    List<T> findAll(Sort sort);

    /**
     * Retrieves a page of entities
     *
     * @param pageable Pagination information
     * @return Page of entities
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Retrieves all entities with the given IDs
     *
     * @param ids IDs to search for
     * @return List of entities matching the IDs
     */
    List<T> findAllByIds(Iterable<Long> ids);

    /**
     * Returns the total number of entities
     *
     * @return Total count
     */
    long count();

    /**
     * Soft deletes the given entity
     *
     * @param entity Entity to delete
     */
    void delete(T entity);

    /**
     * Soft deletes all given entities
     *
     * @param entities Entities to delete
     */
    void deleteAll(Iterable<? extends T> entities);

    /**
     * Soft deletes all entities
     */
    void deleteAll();

    /**
     * Saves the given entity
     *
     * @param entity Entity to save
     * @param <S>    Entity type
     * @return Saved entity
     */
    <S extends T> S save(S entity);

    /**
     * Saves all given entities
     *
     * @param entities Entities to save
     * @param <S>      Entity type
     * @return List of saved entities
     */
    <S extends T> List<S> saveAll(Iterable<S> entities);

    /**
     * Retrieves an entity by its ID
     *
     * @param id Entity ID
     * @return Optional containing the entity if found
     */
    Optional<T> findById(Long id);

    /**
     * Flushes all pending changes to the database
     */
    void flush();

    /**
     * Saves an entity and flushes changes instantly
     *
     * @param entity Entity to save
     * @param <S>    Entity type
     * @return Saved entity
     */
    <S extends T> S saveAndFlush(S entity);

    /**
     * Saves all given entities and flushes changes instantly
     *
     * @param entities Entities to save
     * @param <S>      Entity type
     * @return List of saved entities
     */
    <S extends T> List<S> saveAllAndFlush(Iterable<S> entities);
}