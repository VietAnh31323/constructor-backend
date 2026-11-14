package com.backend.constructor.infras.repository;

import com.backend.constructor.common.base.repository.JpaRepositoryAdapter;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.StaffCertificateEntity;
import com.backend.constructor.core.port.repository.StaffCertificateRepository;
import com.backend.constructor.infras.repository.jpa.StaffCertificateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StaffCertificateRepositoryImpl extends JpaRepositoryAdapter<StaffCertificateEntity> implements StaffCertificateRepository {
    private final StaffCertificateJpaRepository staffCertificateJpaRepository;

    @Override
    public StaffCertificateEntity getCertificateById(Long id) {
        return findById(id).orElseThrow(() -> BusinessException.exception("CST002"));
    }

    @Override
    public List<StaffCertificateEntity> getListByStaffId(Long staffId) {
        if (staffId == null) {
            return Collections.emptyList();
        }
        return staffCertificateJpaRepository.findByStaffIdOrderByExpiryDateAscIdAsc(staffId);
    }

    @Override
    public List<StaffCertificateEntity> getListByStaffIds(Collection<Long> staffIds) {
        if (staffIds == null || staffIds.isEmpty()) {
            return Collections.emptyList();
        }
        return staffCertificateJpaRepository.findByStaffIdInOrderByExpiryDateAscIdAsc(staffIds);
    }
}
