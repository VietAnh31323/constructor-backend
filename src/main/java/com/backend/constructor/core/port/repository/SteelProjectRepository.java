package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface SteelProjectRepository extends BaseRepository<SteelProjectEntity> {
    SteelProjectEntity getSteelProjectById(Long id);

    Page<SteelProjectEntity> getPageSteelProject(String search, Pageable pageable);
}