package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SteelLineRepository extends BaseRepository<SteelLineEntity> {
    List<SteelLineEntity> getListBySteelId(Long steelId);
}