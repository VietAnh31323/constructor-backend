package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.core.domain.entity.SteelProjectEntity;
import com.backend.constructor.core.port.repository.SteelProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SteelProjectRepositoryImpl extends JpaRepositoryAdapter<SteelProjectEntity> implements SteelProjectRepository {
}
