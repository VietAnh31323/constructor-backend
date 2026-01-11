package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelProjectAssemblyMapRepository extends BaseRepository<SteelProjectAssemblyMapEntity> {
    List<SteelProjectAssemblyMapEntity> getListBySteelProjectId(Long steelProjectId);

    SteelProjectAssemblyMapEntity getSteelProjectAssemblyMapById(Long id);
}