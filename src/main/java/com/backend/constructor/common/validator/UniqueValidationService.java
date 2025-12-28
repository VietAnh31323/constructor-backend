package com.backend.constructor.common.validator;

import com.backend.constructor.common.base.entity.BaseEntity;
import com.backend.constructor.common.error.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UniqueValidationService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Thực hiện validate tính duy nhất cho thực thể.
     */
    public void validate(Object entity) {
        if (entity == null) return;

        ReflectionUtils.doWithFields(entity.getClass(), field -> {
            Unique anno = field.getAnnotation(Unique.class);
            if (anno != null) {
                processValidation(entity, field, anno);
            }
        });
    }

    private void processValidation(Object entity, Field field, Unique anno) {
        List<FieldHolder> groupFields = findFieldsByGroup(entity, anno.group(), field);

        if (isDuplicate(entity, groupFields, anno.ignoreCase())) {
            // Thay vì throw RuntimeException chung chung, bạn nên dùng một Custom Exception
            throw BusinessException.exception(anno.err());
        }
    }

    private boolean isDuplicate(Object entity, List<FieldHolder> fields, boolean ignoreCase) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<?> root = query.from(entity.getClass());

        List<Predicate> predicates = buildPredicates(cb, root, fields, ignoreCase);

        // Xử lý logic Update: loại trừ bản ghi hiện tại
        if (entity instanceof BaseEntity base && base.getId() != null) {
            predicates.add(cb.notEqual(root.get("id"), base.getId()));
        }

        query.select(cb.count(root)).where(cb.and(predicates.toArray(new Predicate[0])));

        Long count = entityManager.createQuery(query).getSingleResult();
        return count != null && count > 0;
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<?> root, List<FieldHolder> fields, boolean ignoreCase) {
        List<Predicate> predicates = new ArrayList<>();
        for (FieldHolder holder : fields) {
            Path<String> path = root.get(holder.name());

            if (ignoreCase && holder.value() instanceof String strVal) {
                predicates.add(cb.equal(cb.lower(path), strVal.toLowerCase()));
            } else {
                predicates.add(cb.equal(path, holder.value()));
            }
        }
        return predicates;
    }

    private List<FieldHolder> findFieldsByGroup(Object entity, String[] targetTags, Field currentField) {
        List<FieldHolder> holders = new ArrayList<>();

        if (isSingleFieldValidation(targetTags)) {
            holders.add(new FieldHolder(currentField.getName(), getFieldValue(entity, currentField)));
            return holders;
        }

        ReflectionUtils.doWithFields(entity.getClass(), f -> {
            Unique anno = f.getAnnotation(Unique.class);
            if (anno != null && hasMatchingTag(targetTags, anno.group())) {
                holders.add(new FieldHolder(f.getName(), getFieldValue(entity, f)));
            }
        });
        return holders;
    }

    private boolean isSingleFieldValidation(String[] tags) {
        return tags == null || tags.length == 0 || (tags.length == 1 && tags[0].isEmpty());
    }

    private boolean hasMatchingTag(String[] targetTags, String[] fieldTags) {
        for (String tTag : targetTags) {
            for (String aTag : fieldTags) {
                if (tTag.equals(aTag)) return true;
            }
        }
        return false;
    }

    private Object getFieldValue(Object obj, Field field) {
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, obj);
    }

    private record FieldHolder(String name, Object value) {
    }
}