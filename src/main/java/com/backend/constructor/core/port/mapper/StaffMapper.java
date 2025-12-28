package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.app.dto.staff.StaffOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.StaffEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultConfigMapper.class)
public interface StaffMapper extends EntityMapper<StaffEntity, StaffDto> {
    StaffOutput toOutput(StaffEntity staffEntity);

    @Override
    @Mapping(target = "genPassword", ignore = true)
    void update(StaffDto dto, @MappingTarget StaffEntity entity);
}
