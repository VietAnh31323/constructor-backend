package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.staff.StaffDto;
import com.backend.constructor.app.dto.staff.StaffOutput;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.StaffEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(config = DefaultConfigMapper.class)
public interface StaffMapper extends EntityMapper<StaffEntity, StaffDto> {
    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "accountId", ignore = true)
    StaffEntity toEntity(StaffDto dto);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "accountId", ignore = true)
    StaffDto toDto(StaffEntity entity);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "accountId", ignore = true)
    StaffOutput toOutput(StaffEntity staffEntity);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "genPassword", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountId", ignore = true)
    void update(StaffDto dto, @MappingTarget StaffEntity entity);
}
