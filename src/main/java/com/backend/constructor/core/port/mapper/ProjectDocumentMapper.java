package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.project.ProjectDocumentDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.ProjectDocumentEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface ProjectDocumentMapper extends EntityMapper<ProjectDocumentEntity, ProjectDocumentDto> {
}
