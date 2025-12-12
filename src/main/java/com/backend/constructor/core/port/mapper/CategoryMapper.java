package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.category.CategoryDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface CategoryMapper extends EntityMapper<CategoryEntity, CategoryDto> {
}
