package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.steel_project.SteelProjectDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.SteelProjectEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface SteelProjectMapper extends EntityMapper<SteelProjectEntity, SteelProjectDto> {
}
