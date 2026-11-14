package com.backend.constructor.core.port.repository;

import com.backend.constructor.common.base.repository.BaseRepository;
import com.backend.constructor.core.domain.entity.StaffCertificateEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StaffCertificateRepository extends BaseRepository<StaffCertificateEntity> {
    StaffCertificateEntity getCertificateById(Long id);

    List<StaffCertificateEntity> getListByStaffId(Long staffId);

    List<StaffCertificateEntity> getListByStaffIds(Collection<Long> staffIds);
}
