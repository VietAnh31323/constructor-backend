package com.backend.constructor.common.base.mapper;

import org.mapstruct.*;

import java.util.List;

public interface EntityMapper<E, D> {
    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    E toEntity(D dto);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    D toDto(E entity);

    List<D> toDto(List<E> entities);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    List<D> toEntity(List<D> requests);

    @Named(value = "update")
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    void update(D dto, @MappingTarget E entity);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    void updateDto(@MappingTarget D dto, E entity);
}
