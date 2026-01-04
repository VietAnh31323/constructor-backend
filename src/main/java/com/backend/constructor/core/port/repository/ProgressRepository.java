package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.progress.ProgressFilterParam;
import com.backend.constructor.common.base.dto.response.CodeNameResponse;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.ProgressEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;

@Repository
public interface ProgressRepository extends BaseRepository<ProgressEntity> {
    ProgressEntity getProgressById(Long id);

    Page<ProgressEntity> getPageProgress(ProgressFilterParam param, Pageable pageable);

    Map<Long, CodeNameResponse> getMapProgressSimpleByIds(Set<Long> progressIds);
}