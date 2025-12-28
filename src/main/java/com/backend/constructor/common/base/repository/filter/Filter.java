package com.backend.constructor.common.base.repository.filter;

import com.backend.constructor.common.base.entity.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Filter<T> {
    private static final String UPPER = "upper(";
    private static final String UPPER_FUNCTION_UNACCENT = "upper(function('unaccent',";
    private final TypedQuery<T> query;
    private final TypedQuery<Long> countQuery;
    private final Pageable pageable;
    private final boolean isGroupBy;

    private Filter(TypedQuery<T> query, TypedQuery<Long> countQuery, Pageable pageable, boolean isGroupBy) {
        this.query = query;
        this.countQuery = countQuery;
        this.pageable = pageable;
        this.isGroupBy = isGroupBy;
    }

    public static FilterBuilder builder() {
        return new Builder();
    }

    public static FilterJoiner joiner(Class<? extends BaseEntity> clazz, String fieldName) {
        return new FilterJoiner(clazz, fieldName);
    }

    public static FilterFunction fnc(String functionName, String... fieldNames) {
        return new FilterFunction(functionName, fieldNames);
    }

    public List<T> getList() {
        return query.getResultList();
    }

    public Page<T> getPage() {
        long totalCount;
        if (isGroupBy) {
            totalCount = countQuery.getResultList().size();
        } else {
            totalCount = countQuery.getSingleResult();
        }

        query.setMaxResults(pageable.getPageSize());
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        return new PageImpl<>(getList(), pageable, totalCount);
    }

    @Slf4j
    public static class Builder implements FilterBuilder {
        private static final String AND = " and ";
        private static final String OR = " or ";

        private final StringBuilder select = new StringBuilder();
        private final StringBuilder join = new StringBuilder();
        private final StringBuilder filter = new StringBuilder();
        private final StringBuilder search = new StringBuilder();
        private final StringBuilder paging = new StringBuilder();
        private final List<Object> parameters = new ArrayList<>();

        private Pageable pageable;
        private String table = "e.";
        private int tableCount = 1;
        private EntityManager entityManager;
        private StringBuilder statement;
        private String logicOperator;
        private String groupBy;

        public Builder() {
            this.statement = this.filter;
            this.logicOperator = AND;
            this.groupBy = "";
        }

        @Override
        public FilterBuilder groupBy(@NonNull String rawText) {
            this.groupBy = " group by " + rawText;
            return this;
        }

        @Override
        public FilterBuilder isEqual(String fieldName, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isEqual(FilterFunction function, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotEqual(String fieldName, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.NOT_EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotEqual(FilterFunction function, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.NOT_EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNull(String fieldName) {
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.IS.value())
                    .append("null");
            return this;
        }

        @Override
        public FilterBuilder isNotNull(String fieldName) {
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.IS_NOT.value())
                    .append("null");
            return this;
        }

        @Override
        public FilterBuilder isIn(String fieldName, Collection<?> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }
            parameters.add(values);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.IN.value())
                    .append("(?")
                    .append(parameters.size())
                    .append(")");
            return this;
        }

        @Override
        public FilterBuilder isIn(FilterFunction function, Collection<?> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }
            parameters.add(values);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.IN.value())
                    .append("(?")
                    .append(parameters.size())
                    .append(")");
            return this;
        }

        @Override
        public FilterBuilder isNotIn(String fieldName, Collection<?> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }
            parameters.add(values);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.NOT_IN.value())
                    .append("(?")
                    .append(parameters.size())
                    .append(")");
            return this;
        }

        @Override
        public FilterBuilder isNotIn(FilterFunction function, Collection<?> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }
            parameters.add(values);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.NOT_IN.value())
                    .append("(?")
                    .append(parameters.size())
                    .append(")");
            return this;
        }

        @Override
        public FilterBuilder isContains(String fieldName, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isContains(FilterFunction function, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isContains(String fieldName, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            applyFilterFlag(value, fieldName, parameters.size(), FilterOperator.LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isContains(FilterFunction function, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            applyFilterFlag(value, function, parameters.size(), FilterOperator.LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isNotContains(String fieldName, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.NOT_LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotContains(FilterFunction function, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.NOT_LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotContains(String fieldName, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            applyFilterFlag(value, fieldName, parameters.size(), FilterOperator.NOT_LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isNotContains(FilterFunction function, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value) + "%");
            applyFilterFlag(value, function, parameters.size(), FilterOperator.NOT_LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isStartWith(String fieldName, String value) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isStartWith(FilterFunction function, String value) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isStartWith(String fieldName, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            applyFilterFlag(value, fieldName, parameters.size(), FilterOperator.LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isStartWith(FilterFunction function, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            applyFilterFlag(value, function, parameters.size(), FilterOperator.LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isNotStartWith(String fieldName, String value) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.NOT_LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotStartWith(FilterFunction function, String value) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.NOT_LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotStartWith(String fieldName, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            applyFilterFlag(value, fieldName, parameters.size(), FilterOperator.NOT_LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isNotStartWith(FilterFunction function, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add(escape(value) + "%");
            applyFilterFlag(value, function, parameters.size(), FilterOperator.NOT_LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isEndWith(String fieldName, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isEndWith(FilterFunction function, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isEndWith(String fieldName, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            applyFilterFlag(value, fieldName, parameters.size(), FilterOperator.LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isEndWith(FilterFunction function, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            applyFilterFlag(value, function, parameters.size(), FilterOperator.LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isNotEndWith(String fieldName, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.NOT_LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotEndWith(FilterFunction function, String value) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            statement.append(logicOperator)
                    .append(function)
                    .append(FilterOperator.NOT_LIKE.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isNotEndWith(String fieldName, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            applyFilterFlag(value, fieldName, parameters.size(), FilterOperator.NOT_LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isNotEndWith(FilterFunction function, String value, FilterFlag flag) {
            if (value == null) {
                return this;
            }
            parameters.add("%" + escape(value));
            applyFilterFlag(value, function, parameters.size(), FilterOperator.NOT_LIKE, flag);
            return this;
        }

        @Override
        public FilterBuilder isLessThan(String fieldName, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.LESS_THAN.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isLessThan(FilterFunction function, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.LESS_THAN.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isGreaterThan(String fieldName, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.GREATER_THAN.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isGreaterThan(FilterFunction function, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.GREATER_THAN.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isLessThanOrEqual(String fieldName, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.LESS_THAN_OR_EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isLessThanOrEqual(FilterFunction function, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.LESS_THAN_OR_EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isGreaterThanOrEqual(String fieldName, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(table)
                    .append(fieldName)
                    .append(FilterOperator.GREATER_THAN_OR_EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder isGreaterThanOrEqual(FilterFunction function, Object value) {
            if (value == null) {
                return this;
            }
            parameters.add(value);
            statement.append(logicOperator)
                    .append(getRawText(table, function))
                    .append(FilterOperator.GREATER_THAN_OR_EQUAL.value())
                    .append("?")
                    .append(parameters.size());
            return this;
        }

        @Override
        public FilterBuilder pageable(Pageable pageable) {
            this.pageable = pageable;
            Sort sort = pageable.getSort();

            if (sort.isSorted()) {
                paging.append(" order by ");
                for (Sort.Order order : sort) {
                    String property = order.getProperty();
                    if (property.startsWith("!")) {
                        paging.append(property.substring(1))
                                .append(" ")
                                .append(order.getDirection().name())
                                .append(",");
                    } else {
                        paging.append("e.")
                                .append(property)
                                .append(" ")
                                .append(order.getDirection().name())
                                .append(",");
                    }
                }
                paging.deleteCharAt(paging.length() - 1);
            }
            return this;
        }

        @Override
        public FilterBuilder select(String fieldName) {
            select.append(",").append(table).append(fieldName);
            return this;
        }

        @Override
        public FilterBuilder select(FilterFunction function) {
            select.append(",").append(getRawText(table, function));
            return this;
        }

        @Override
        public FilterBuilder as(String alias) {
            select.append(" as ").append(alias);
            return this;
        }

        @Override
        public FilterBuilder selectAll() {
            select.append(",").append(table, 0, table.length() - 1);
            return this;
        }

        @Override
        public FilterBuilder search() {
            this.statement = this.search;
            this.logicOperator = OR;
            return this;
        }

        @Override
        public FilterBuilder filter() {
            this.statement = this.filter;
            this.logicOperator = AND;
            return this;
        }

        @Override
        public FilterBuilder innerJoin(String fieldName, FilterJoiner joiner) {
            String alias = "e" + tableCount++;
            this.table = alias + ".";
            join.append(" join ")
                    .append(joiner.getClazz().getSimpleName())
                    .append(" ")
                    .append(alias)
                    .append(" on e.")
                    .append(fieldName)
                    .append(" = ")
                    .append(table)
                    .append(joiner.getFieldName());
            return this;
        }

        @Override
        public FilterBuilder thenInnerJoin(String fieldName, FilterJoiner joiner) {
            String alias = "e" + tableCount++;
            join.append(" join ")
                    .append(joiner.getClazz().getSimpleName())
                    .append(" ")
                    .append(alias)
                    .append(" on ")
                    .append(table)
                    .append(fieldName);
            this.table = alias + ".";
            join.append(" = ").append(table).append(joiner.getFieldName());
            return this;
        }

        @Override
        public FilterBuilder leftJoin(String fieldName, FilterJoiner joiner) {
            String alias = "e" + tableCount++;
            this.table = alias + ".";
            join.append(" left join ")
                    .append(joiner.getClazz().getSimpleName())
                    .append(" ")
                    .append(alias)
                    .append(" on e.")
                    .append(fieldName)
                    .append(" = ")
                    .append(table)
                    .append(joiner.getFieldName());
            return this;
        }

        @Override
        public FilterBuilder thenLeftJoin(String fieldName, FilterJoiner joiner) {
            String alias = "e" + tableCount++;
            join.append(" left join ")
                    .append(joiner.getClazz().getSimpleName())
                    .append(" ")
                    .append(alias)
                    .append(" on ")
                    .append(table)
                    .append(fieldName);
            this.table = alias + ".";
            join.append(" = ").append(table).append(joiner.getFieldName());
            return this;
        }

        @Override
        public FilterBuilder withContext(EntityManager entityManager) {
            this.entityManager = entityManager;
            return this;
        }

        @Override
        public <T extends BaseEntity> Filter<T> build(Class<T> entity) {
            if (entityManager == null) {
                throw new FilterBuilderException("The persistence context is required!");
            }

            StringBuilder selectClause = new StringBuilder()
                    .append("select e")
                    .append(" from ")
                    .append(entity.getSimpleName())
                    .append(" e")
                    .append(join);

            StringBuilder whereClause = buildingWhereClause();
            TypedQuery<T> query = createQuery(
                    selectClause.append(whereClause).append(groupBy).append(paging),
                    entity
            );

            TypedQuery<Long> countQuery = createCountQuery(entity, join, whereClause, groupBy);
            return new Filter<>(query, countQuery, pageable, !StringUtils.isBlank(groupBy));
        }

        @Override
        public <T> Filter<T> build(Class<? extends BaseEntity> entity, Class<T> dto) {
            if (entityManager == null) {
                throw new FilterBuilderException("The persistence context is required!");
            }
            if (select.isEmpty()) {
                throw new FilterBuilderException("Select statements is required!");
            }

            StringBuilder selectClause = new StringBuilder()
                    .append("select new ")
                    .append(dto.getName())
                    .append("(")
                    .append(select.deleteCharAt(0))
                    .append(")")
                    .append(" from ")
                    .append(entity.getSimpleName())
                    .append(" e")
                    .append(join);

            StringBuilder whereClause = buildingWhereClause();
            TypedQuery<T> query = createQuery(
                    selectClause.append(whereClause).append(groupBy).append(paging),
                    dto
            );

            TypedQuery<Long> countQuery = createCountQuery(entity, join, whereClause, groupBy);
            return new Filter<>(query, countQuery, pageable, !StringUtils.isBlank(groupBy));
        }

        private TypedQuery<Long> createCountQuery(
                Class<? extends BaseEntity> entity,
                StringBuilder joinClause,
                StringBuilder whereClause,
                String groupBy) {
            StringBuilder builder = new StringBuilder("select count(e) from ")
                    .append(entity.getSimpleName())
                    .append(" e")
                    .append(joinClause)
                    .append(whereClause)
                    .append(groupBy);
            return createQuery(builder, Long.class);
        }

        private <T> TypedQuery<T> createQuery(StringBuilder queryBuilder, Class<T> clazz) {
            String stringQuery = queryBuilder.toString();
            log.debug(stringQuery);

            TypedQuery<T> query = entityManager.createQuery(stringQuery, clazz);
            for (int i = 0; i < parameters.size(); i++) {
                query.setParameter(i + 1, parameters.get(i));
            }
            return query;
        }

        private StringBuilder buildingWhereClause() {
            StringBuilder whereClause = new StringBuilder();

            if (search.isEmpty() && filter.isEmpty()) {
                return whereClause;
            }

            whereClause.append(" where ");

            if (!search.isEmpty()) {
                whereClause.append("(")
                        .append(search.delete(0, 4))
                        .append(")");
            }

            if (!filter.isEmpty()) {
                if (search.isEmpty()) {
                    filter.delete(0, 4);
                }
                whereClause.append(filter);
            }

            return whereClause;
        }

        private void applyFilterFlag(
                String value,
                String fieldName,
                int position,
                FilterOperator operator,
                FilterFlag flag) {
            switch (flag) {
                case FilterFlag.CASE_SENSITIVE -> applyCaseSensitive(fieldName, position, operator);
                case FilterFlag.UNACCENT -> applyUnaccent(value, fieldName, position, operator);
                default -> applyUnaccentCaseSensitive(value, fieldName, position, operator);
            }
        }

        private void applyCaseSensitive(String fieldName, int position, FilterOperator operator) {
            statement.append(logicOperator)
                    .append(UPPER)
                    .append(table)
                    .append(fieldName)
                    .append(")")
                    .append(operator.value())
                    .append(UPPER)
                    .append("?")
                    .append(position)
                    .append(")");
        }

        private void applyUnaccent(String value, String fieldName, int position, FilterOperator operator) {
            if (StringUtils.stripAccents(value).equals(value)) {
                statement.append(logicOperator)
                        .append("function('unaccent',")
                        .append(table)
                        .append(fieldName)
                        .append(")")
                        .append(operator.value())
                        .append("?")
                        .append(position);
            } else {
                statement.append(logicOperator)
                        .append(table)
                        .append(fieldName)
                        .append(operator.value())
                        .append("?")
                        .append(position);
            }
        }

        private void applyUnaccentCaseSensitive(String value, String fieldName, int position, FilterOperator operator) {
            if (StringUtils.stripAccents(value).equals(value)) {
                statement.append(logicOperator)
                        .append(UPPER_FUNCTION_UNACCENT)
                        .append(table)
                        .append(fieldName)
                        .append("))")
                        .append(operator.value())
                        .append(UPPER_FUNCTION_UNACCENT)
                        .append("?")
                        .append(position)
                        .append("))");
            } else {
                applyCaseSensitive(fieldName, position, operator);
            }
        }

        private void applyFilterFlag(
                String value,
                FilterFunction function,
                int position,
                FilterOperator operator,
                FilterFlag flag) {
            switch (flag) {
                case FilterFlag.CASE_SENSITIVE -> applyCaseSensitive(function, position, operator);
                case FilterFlag.UNACCENT -> applyUnaccent(value, function, position, operator);
                default -> applyUnaccentCaseSensitive(value, function, position, operator);
            }
        }

        private void applyCaseSensitive(FilterFunction function, int position, FilterOperator operator) {
            statement.append(logicOperator)
                    .append(UPPER)
                    .append(getRawText(table, function))
                    .append(")")
                    .append(operator.value())
                    .append(UPPER)
                    .append("?")
                    .append(position)
                    .append(")");
        }

        private void applyUnaccent(String value, FilterFunction function, int position, FilterOperator operator) {
            if (StringUtils.stripAccents(value).equals(value)) {
                statement.append(logicOperator)
                        .append("function('unaccent',")
                        .append(getRawText(table, function))
                        .append(")")
                        .append(operator.value())
                        .append("?")
                        .append(position);
            } else {
                statement.append(logicOperator)
                        .append(getRawText(table, function))
                        .append(operator.value())
                        .append("?")
                        .append(position);
            }
        }

        private void applyUnaccentCaseSensitive(String value, FilterFunction function, int position, FilterOperator operator) {
            if (StringUtils.stripAccents(value).equals(value)) {
                statement.append(logicOperator)
                        .append(UPPER_FUNCTION_UNACCENT)
                        .append(getRawText(table, function))
                        .append("))")
                        .append(operator.value())
                        .append(UPPER)
                        .append("?")
                        .append(position)
                        .append(")");
            } else {
                applyCaseSensitive(function, position, operator);
            }
        }

        private String getRawText(String table, FilterFunction function) {
            StringBuilder builder = new StringBuilder();
            builder.append(function.getFunctionName()).append("(");

            for (String fieldName : function.getFieldNames()) {
                if (fieldName.startsWith("s#")) {
                    builder.append("'")
                            .append(fieldName.substring(2))
                            .append("'")
                            .append(",");
                } else if (fieldName.startsWith("n#")) {
                    builder.append(fieldName.substring(2)).append(",");
                } else {
                    builder.append(table).append(fieldName).append(",");
                }
            }

            builder.deleteCharAt(builder.length() - 1);
            builder.append(")");
            return builder.toString();
        }

        private String escape(@NonNull String value) {
            return value.replace("%", "\\%").replace("_", "\\_");
        }
    }
}