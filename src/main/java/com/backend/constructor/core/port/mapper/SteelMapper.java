package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.steel.SteelDto;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.SteelEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface SteelMapper extends EntityMapper<SteelEntity, SteelDto> {
    SteelOutput toOutput(SteelEntity steelEntity);
}
