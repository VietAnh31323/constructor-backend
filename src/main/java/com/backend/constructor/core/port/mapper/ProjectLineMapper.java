package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.project.ProjectLineDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.ProjectLineEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface ProjectLineMapper extends EntityMapper<ProjectLineEntity, ProjectLineDto> {
}
