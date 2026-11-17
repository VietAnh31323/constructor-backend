package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProcessTemplateEntity;
import com.backend.constructor.core.domain.enums.BuildingType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessTemplateJpaRepository extends BaseJpaRepository<ProcessTemplateEntity> {
    List<ProcessTemplateEntity> findAllByOrderByIdDesc();

    List<ProcessTemplateEntity> findAllByBuildingTypeOrderByIsDefaultDescIdDesc(BuildingType buildingType);

    Optional<ProcessTemplateEntity> findFirstByBuildingTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(BuildingType buildingType);

    Optional<ProcessTemplateEntity> findByCode(String code);
}
