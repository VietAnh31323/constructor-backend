
package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.progress.ProgressDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.ProgressEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface ProgressMapper extends EntityMapper<ProgressEntity, ProgressDto> {
}
