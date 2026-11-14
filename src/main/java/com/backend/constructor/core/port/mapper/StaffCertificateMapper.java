package com.backend.constructor.core.port.mapper;

import com.backend.constructor.app.dto.staff.StaffCertificateDto;
import com.backend.constructor.common.base.mapper.DefaultConfigMapper;
import com.backend.constructor.common.base.mapper.EntityMapper;
import com.backend.constructor.core.domain.entity.StaffCertificateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = DefaultConfigMapper.class)
public interface StaffCertificateMapper extends EntityMapper<StaffCertificateEntity, StaffCertificateDto> {
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "staffId", ignore = true)
    void update(StaffCertificateDto dto, @MappingTarget StaffCertificateEntity entity);
}
