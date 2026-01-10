package com.backend.constructor.core.domain.converter;

import com.backend.constructor.common.base.converter.JsonCollectionConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class LongListConverter extends JsonCollectionConverter<Long, List<Long>> {
    @Override
    protected Class<Long> getElementClass() {
        return Long.class;
    }

    @Override
    protected List<Long> createCollection() {
        return new ArrayList<>();
    }
}
