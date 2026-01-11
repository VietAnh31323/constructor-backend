package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelProjectEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface SteelProjectRepository extends BaseRepository<SteelProjectEntity> {
    SteelProjectEntity getSteelProjectById(Long id);
}