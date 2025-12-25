package com.backend.constructor.core.domain.converter;

import com.backend.constructor.app.dto.upload.UploadDto;
import com.backend.constructor.common.base.converter.JsonListConverter;
import jakarta.persistence.Converter;

@Converter
public class UploadDtoListConverter extends JsonListConverter<UploadDto> {
    @Override
    protected Class<UploadDto> getClazz() {
        return UploadDto.class;
    }
}
