package com.backend.constructor.common.base.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collection;

@Converter
public abstract class JsonCollectionConverter<T, C extends Collection<T>> implements AttributeConverter<C, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Class của phần tử (T)
     */
    protected abstract Class<T> getElementClass();

    /**
     * Collection concrete để khởi tạo khi đọc DB (ArrayList, HashSet, ...)
     */
    protected abstract C createCollection();

    @Override
    public String convertToDatabaseColumn(C attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert collection to JSON", e);
        }
    }

    @Override
    public C convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return createCollection();
        }
        try {
            return objectMapper.readValue(
                    dbData,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(createCollection().getClass(), getElementClass())
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert JSON to collection", e);
        }
    }
}
