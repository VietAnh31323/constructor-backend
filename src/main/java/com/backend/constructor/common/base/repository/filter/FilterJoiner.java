package com.backend.constructor.common.base.repository.filter;

import com.backend.constructor.common.base.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilterJoiner {
    private Class<? extends BaseEntity> clazz;
    private String fieldName;
}