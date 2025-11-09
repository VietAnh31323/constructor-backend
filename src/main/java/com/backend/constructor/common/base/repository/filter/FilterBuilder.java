package com.backend.constructor.common.base.repository.filter;

import com.backend.constructor.common.base.entity.BaseEntity;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Collection;

public interface FilterBuilder {
    FilterBuilder groupBy(@NonNull String rawText);

    FilterBuilder isEqual(String fieldName, Object value);

    FilterBuilder isEqual(FilterFunction function, Object value);

    FilterBuilder isNotEqual(String fieldName, Object value);

    FilterBuilder isNotEqual(FilterFunction function, Object value);

    FilterBuilder isNull(String fieldName);

    FilterBuilder isNotNull(String fieldName);

    FilterBuilder isIn(String fieldName, Collection<?> values);

    FilterBuilder isIn(FilterFunction function, Collection<?> values);

    FilterBuilder isNotIn(String fieldName, Collection<?> values);

    FilterBuilder isNotIn(FilterFunction function, Collection<?> values);

    FilterBuilder isContains(String fieldName, String value);

    FilterBuilder isContains(FilterFunction function, String value);

    FilterBuilder isContains(String fieldName, String value, FilterFlag flag);

    FilterBuilder isContains(FilterFunction function, String value, FilterFlag flag);

    FilterBuilder isNotContains(String fieldName, String value);

    FilterBuilder isNotContains(FilterFunction function, String value);

    FilterBuilder isNotContains(String fieldName, String value, FilterFlag flag);

    FilterBuilder isNotContains(FilterFunction function, String value, FilterFlag flag);

    FilterBuilder isStartWith(String fieldName, String value);

    FilterBuilder isStartWith(FilterFunction function, String value);

    FilterBuilder isStartWith(String fieldName, String value, FilterFlag flag);

    FilterBuilder isStartWith(FilterFunction function, String value, FilterFlag flag);

    FilterBuilder isNotStartWith(String fieldName, String value);

    FilterBuilder isNotStartWith(FilterFunction function, String value);

    FilterBuilder isNotStartWith(String fieldName, String value, FilterFlag flag);

    FilterBuilder isNotStartWith(FilterFunction function, String value, FilterFlag flag);

    FilterBuilder isEndWith(String fieldName, String value);

    FilterBuilder isEndWith(FilterFunction function, String value);

    FilterBuilder isEndWith(String fieldName, String value, FilterFlag flag);

    FilterBuilder isEndWith(FilterFunction function, String value, FilterFlag flag);

    FilterBuilder isNotEndWith(String fieldName, String value);

    FilterBuilder isNotEndWith(FilterFunction function, String value);

    FilterBuilder isNotEndWith(String fieldName, String value, FilterFlag flag);

    FilterBuilder isNotEndWith(FilterFunction function, String value, FilterFlag flag);

    FilterBuilder isLessThan(String fieldName, Object value);

    FilterBuilder isLessThan(FilterFunction function, Object value);

    FilterBuilder isGreaterThan(String fieldName, Object value);

    FilterBuilder isGreaterThan(FilterFunction function, Object value);

    FilterBuilder isLessThanOrEqual(String fieldName, Object value);

    FilterBuilder isLessThanOrEqual(FilterFunction function, Object value);

    FilterBuilder isGreaterThanOrEqual(String fieldName, Object value);

    FilterBuilder isGreaterThanOrEqual(FilterFunction function, Object value);

    FilterBuilder pageable(Pageable pageable);

    FilterBuilder select(String fieldName);

    FilterBuilder select(FilterFunction function);

    FilterBuilder as(String alias);

    FilterBuilder selectAll();

    FilterBuilder search();

    FilterBuilder filter();

    FilterBuilder innerJoin(String fieldName, FilterJoiner joiner);

    FilterBuilder thenInnerJoin(String fieldName, FilterJoiner joiner);

    FilterBuilder leftJoin(String fieldName, FilterJoiner joiner);

    FilterBuilder thenLeftJoin(String fieldName, FilterJoiner joiner);

    FilterBuilder withContext(EntityManager entityManager);

    <T extends BaseEntity> Filter<T> build(Class<T> entity);

    <T> Filter<T> build(Class<? extends BaseEntity> entity, Class<T> dto);
}
