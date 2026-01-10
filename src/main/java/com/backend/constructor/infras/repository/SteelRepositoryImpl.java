package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.SteelEntity;
import com.backend.constructor.core.port.repository.SteelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SteelRepositoryImpl extends JpaRepositoryAdapter<SteelEntity> implements SteelRepository {
    @Override
    public SteelEntity getSteelById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }
}
