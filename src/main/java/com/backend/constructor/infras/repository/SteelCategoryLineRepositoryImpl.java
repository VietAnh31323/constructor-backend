package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.SteelCategoryLineEntity;
import com.backend.constructor.core.port.repository.SteelCategoryLineRepository;
import com.backend.constructor.infras.repository.jpa.SteelCategoryLineJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SteelCategoryLineRepositoryImpl extends JpaRepositoryAdapter<SteelCategoryLineEntity> implements SteelCategoryLineRepository {
    private final SteelCategoryLineJpaRepository steelCategoryLineJpaRepository;

    @Override
    public List<SteelCategoryLineEntity> getListByCategoryId(Long steelCategoryId) {
        return steelCategoryLineJpaRepository.findAllBySteelCategoryId(steelCategoryId);
    }
}
