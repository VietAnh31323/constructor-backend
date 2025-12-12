package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.customer.CustomerDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.CustomerEntity;
import org.mapstruct.Mapper;

@Mapper(config = DefaultConfigMapper.class)
public interface CustomerMapper extends EntityMapper<CustomerEntity, CustomerDto> {
}
