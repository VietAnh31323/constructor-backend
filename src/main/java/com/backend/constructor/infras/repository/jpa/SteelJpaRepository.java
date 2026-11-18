package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.SteelEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SteelJpaRepository extends BaseJpaRepository<SteelEntity> {
    List<SteelEntity> findAllByAssemblyMapIdInOrderBySortOrderAscIdAsc(Collection<Long> assemblyMapIds);

    List<SteelEntity> findAllByAssemblyMapIdOrderBySortOrderAscIdAsc(Long assemblyMapId);

    void deleteAllByAssemblyMapIdIn(Collection<Long> assemblyMapIds);
}
