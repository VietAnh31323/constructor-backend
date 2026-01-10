package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.SteelLineEntity;
import com.backend.constructor.core.port.repository.SteelLineRepository;
import com.backend.constructor.infras.repository.jpa.SteelLineJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SteelLineRepositoryImpl extends JpaRepositoryAdapter<SteelLineEntity> implements SteelLineRepository {
    private final SteelLineJpaRepository steelLineJpaRepository;

    @Override
    public List<SteelLineEntity> getListBySteelId(Long steelId) {
        return steelLineJpaRepository.findAllBySteelId(steelId);
    }
}
