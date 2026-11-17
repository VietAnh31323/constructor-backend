package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProcessTemplateStageEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProcessTemplateStageJpaRepository extends BaseJpaRepository<ProcessTemplateStageEntity> {
    List<ProcessTemplateStageEntity> findAllByTemplateIdOrderBySortOrderAscIdAsc(Long templateId);

    List<ProcessTemplateStageEntity> findAllByTemplateIdIn(Collection<Long> templateIds);

    void deleteAllByTemplateId(Long templateId);
}
