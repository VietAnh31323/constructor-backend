package com.backend.constructor.common.base.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public abstract class JsonListConverter<T> implements AttributeConverter<List<T>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected abstract Class<T> getClazz();

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert list to JSON", e);
        }
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    dbData, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, getClazz())
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert JSON to list", e);
        }
    }
}
