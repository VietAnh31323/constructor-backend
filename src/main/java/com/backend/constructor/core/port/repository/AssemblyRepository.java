package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.AssemblyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AssemblyRepository extends BaseRepository<AssemblyEntity> {
    Page<AssemblyEntity> getPageAssembly(String search, Pageable pageable);
}