package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.steel_category.SteelCategoryDto;
import com.backend.constructor.app.dto.steel_category.SteelCategoryOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.SteelCategoryEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface SteelCategoryMapper extends EntityMapper<SteelCategoryEntity, SteelCategoryDto> {
    SteelCategoryOutput toOutput(SteelCategoryEntity steelCategoryEntity);
}
