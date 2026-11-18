package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SteelLineJpaRepository extends BaseJpaRepository<SteelLineEntity> {
    List<SteelLineEntity> findAllBySteelId(Long steelId);

    List<SteelLineEntity> findAllBySteelIdIn(Collection<Long> steelIds);

    void deleteAllBySteelId(Long steelId);

    void deleteAllBySteelIdIn(Collection<Long> steelIds);
}
