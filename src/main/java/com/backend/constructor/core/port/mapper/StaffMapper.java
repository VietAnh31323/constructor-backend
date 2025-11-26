package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.StaffDto;
import com.backend.constructor.app.dto.StaffOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.StaffEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface StaffMapper extends EntityMapper<StaffEntity, StaffDto> {
    StaffOutput toOutput(StaffEntity staffEntity);
}
