package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.project.ProjectDto;
import com.backend.constructor.app.dto.project.ProjectOutput;
import com.backend.constructor.app.dto.project_progress.ProjectInfoDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(config = DefaultConfigMapper.class)
public interface ProjectMapper extends EntityMapper<ProjectEntity, ProjectDto> {
    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "supporterId", source = "supporter.id")
    ProjectEntity toEntity(ProjectDto dto);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    ProjectDto toDto(ProjectEntity entity);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "previousState", ignore = true)
    @Mapping(target = "stateReason", ignore = true)
    @Mapping(target = "stateChangedAt", ignore = true)
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "supporterId", source = "supporter.id")
    void update(ProjectDto dto, @MappingTarget ProjectEntity entity);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    ProjectOutput toOutput(ProjectEntity entity);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    ProjectInfoDto toInfoDto(ProjectEntity entity);
}
