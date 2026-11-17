package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProcessTemplateTaskEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProcessTemplateTaskJpaRepository extends BaseJpaRepository<ProcessTemplateTaskEntity> {
    List<ProcessTemplateTaskEntity> findAllByStageIdInOrderBySortOrderAscIdAsc(Collection<Long> stageIds);

    void deleteAllByStageIdIn(Collection<Long> stageIds);
}
