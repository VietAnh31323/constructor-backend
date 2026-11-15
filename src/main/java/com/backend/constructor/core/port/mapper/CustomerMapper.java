package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(config = DefaultConfigMapper.class)
public interface CustomerMapper extends EntityMapper<CustomerEntity, CustomerDto> {
    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    CustomerEntity toEntity(CustomerDto dto);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    CustomerDto toDto(CustomerEntity entity);

    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(CustomerDto dto, @MappingTarget CustomerEntity entity);
}
