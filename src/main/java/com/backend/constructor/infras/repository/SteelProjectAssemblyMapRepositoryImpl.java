package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.SteelProjectAssemblyMapEntity;
import com.backend.constructor.core.port.repository.SteelProjectAssemblyMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SteelProjectAssemblyMapRepositoryImpl extends JpaRepositoryAdapter<SteelProjectAssemblyMapEntity> implements SteelProjectAssemblyMapRepository {
}
