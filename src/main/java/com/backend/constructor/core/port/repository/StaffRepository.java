package com.backend.constructor.core.port.repository;

import com.backend.constructor.app.dto.StaffFilterParam;
import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.StaffEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffRepository extends BaseRepository<StaffEntity> {
    StaffEntity getStaffById(Long id);

    Page<StaffEntity> getPageStaff(StaffFilterParam param, Pageable pageable);
}