package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelProjectAssemblyMapJpaRepository extends BaseJpaRepository<SteelProjectAssemblyMapEntity> {
    List<SteelProjectAssemblyMapEntity> findAllBySteelProjectId(Long steelProjectId);
}
