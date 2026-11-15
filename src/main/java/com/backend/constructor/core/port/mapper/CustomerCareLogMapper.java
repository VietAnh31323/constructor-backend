package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.customer.CustomerCareLogDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.CustomerCareLogEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(config = DefaultConfigMapper.class)
public interface CustomerCareLogMapper extends EntityMapper<CustomerCareLogEntity, CustomerCareLogDto> {
    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    CustomerCareLogEntity toEntity(CustomerCareLogDto dto);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    CustomerCareLogDto toDto(CustomerCareLogEntity entity);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void update(CustomerCareLogDto dto, @MappingTarget CustomerCareLogEntity entity);
}
