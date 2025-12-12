package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.progress.ProgressDto;
import com.backend.constructor.app.dto.progress.ProgressFilterParam;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProgressApi {
    IdResponse create(ProgressDto input);

    IdResponse update(ProgressDto input);

    IdResponse delete(Long id);

    ProgressDto getDetail(Long id);

    Page<ProgressDto> getListStaff(ProgressFilterParam param,
                                   Pageable pageable);
}
