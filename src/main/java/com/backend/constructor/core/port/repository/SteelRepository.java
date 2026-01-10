package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface SteelRepository extends BaseRepository<SteelEntity> {
    SteelEntity getSteelById(Long id);
}