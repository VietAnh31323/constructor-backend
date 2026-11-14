package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.StaffCertificateEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StaffCertificateJpaRepository extends BaseJpaRepository<StaffCertificateEntity> {
    List<StaffCertificateEntity> findByStaffIdOrderByExpiryDateAscIdAsc(Long staffId);

    List<StaffCertificateEntity> findByStaffIdInOrderByExpiryDateAscIdAsc(Collection<Long> staffIds);
}
