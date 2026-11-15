package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectJpaRepository extends BaseJpaRepository<ProjectEntity> {
    @Query("""
                SELECT p
                FROM ProjectEntity p
                WHERE p.creatorId = :staffId
                   OR p.managerId = :staffId
                   OR p.supporterId = :staffId
                ORDER BY p.id DESC
            """)
    List<ProjectEntity> findAllByParticipantStaffId(Long staffId);

    @Query("""
                SELECT p
                FROM ProjectEntity p
                WHERE p.creatorId IN :staffIds
                   OR p.managerId IN :staffIds
                   OR p.supporterId IN :staffIds
            """)
    List<ProjectEntity> findAllByParticipantStaffIds(Collection<Long> staffIds);

    List<ProjectEntity> findByCustomerIdOrderByIdDesc(Long customerId);

    List<ProjectEntity> findByCustomerIdInOrderByIdDesc(Collection<Long> customerIds);

    boolean existsByCustomerId(Long customerId);
}
