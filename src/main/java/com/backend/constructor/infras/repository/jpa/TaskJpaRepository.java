package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.TaskEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskJpaRepository extends BaseJpaRepository<TaskEntity> {
    List<TaskEntity> findAllByParentId(Long parentId);

    List<TaskEntity> findAllByReviewerId(Long reviewerId);

    List<TaskEntity> findAllByReviewerIdIn(Collection<Long> reviewerIds);
}
