package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import com.backend.constructor.core.port.repository.SteelProjectAssemblyMapRepository;
import com.backend.constructor.infras.repository.jpa.SteelProjectAssemblyMapJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SteelProjectAssemblyMapRepositoryImpl extends JpaRepositoryAdapter<SteelProjectAssemblyMapEntity> implements SteelProjectAssemblyMapRepository {
    private final SteelProjectAssemblyMapJpaRepository steelProjectAssemblyMapJpaRepository;

    @Override
    public List<SteelProjectAssemblyMapEntity> getListBySteelProjectId(Long steelProjectId) {
        return steelProjectAssemblyMapJpaRepository.findAllBySteelProjectId(steelProjectId);
    }
}
