package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.project.ProjectDto;
import com.backend.constructor.app.dto.project.ProjectOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultConfigMapper.class)
public interface ProjectMapper extends EntityMapper<ProjectEntity, ProjectDto> {
    @Override
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "supporterId", source = "supporter.id")
    ProjectEntity toEntity(ProjectDto dto);

    @Override
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "supporterId", source = "supporter.id")
    void update(ProjectDto dto, @MappingTarget ProjectEntity entity);

    ProjectOutput toOutput(ProjectEntity entity);
}
