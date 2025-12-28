package com.backend.constructor.common.service;

import com.backend.constructor.common.base.entity.BaseEntity;

public interface GenerateCodeService {
    void generateCode(Object input, String featureCode, Class<? extends BaseEntity> entityClazz);
}
