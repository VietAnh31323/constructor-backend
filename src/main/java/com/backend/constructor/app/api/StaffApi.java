package com.backend.constructor.app.api;

import com.backend.constructor.app.dto.StaffDto;
import com.backend.constructor.app.dto.StaffFilterParam;
import com.backend.constructor.app.dto.StaffOutput;
import com.backend.constructor.common.base.dto.response.IdResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffApi {
    IdResponse create(StaffDto input);

    IdResponse update(StaffDto input);

    IdResponse delete(Long id);

    StaffDto getDetail(Long id);

    Page<StaffOutput> getListStaff(StaffFilterParam param,
                                   Pageable pageable);
}
