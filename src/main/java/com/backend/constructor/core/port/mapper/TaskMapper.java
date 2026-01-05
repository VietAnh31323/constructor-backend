package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.task.TaskDto;
import com.backend.constructor.app.dto.task.TaskOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = DefaultConfigMapper.class)
public interface TaskMapper extends EntityMapper<TaskEntity, TaskDto> {
    @Override
    @Mapping(target = "parentId", source = "parent.id")
    TaskEntity toEntity(TaskDto dto);

    TaskOutput toOutput(TaskEntity entity);
}
