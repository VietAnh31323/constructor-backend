package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjectJpaRepository extends BaseJpaRepository<ProjectEntity> {
    @Query("""
                SELECT DISTINCT p
                FROM ProjectEntity p
                LEFT JOIN ProjectMemberEntity pm ON pm.projectId = p.id AND pm.deletedAt IS NULL
                WHERE p.deletedAt IS NULL
                  AND (pm.staffId = :staffId
                       OR p.creatorId = :staffId
                       OR p.managerId = :staffId
                       OR p.supporterId = :staffId)
                ORDER BY p.id DESC
            """)
    List<ProjectEntity> findAllByParticipantStaffId(@Param("staffId") Long staffId);

    @Query("""
                SELECT DISTINCT p
                FROM ProjectEntity p
                LEFT JOIN ProjectMemberEntity pm ON pm.projectId = p.id AND pm.deletedAt IS NULL
                WHERE p.deletedAt IS NULL
                  AND (pm.staffId IN :staffIds
                       OR p.creatorId IN :staffIds
                       OR p.managerId IN :staffIds
                       OR p.supporterId IN :staffIds)
            """)
    List<ProjectEntity> findAllByParticipantStaffIds(@Param("staffIds") Collection<Long> staffIds);

    @Query("""
                SELECT DISTINCT p
                FROM ProjectEntity p
                LEFT JOIN ProjectMemberEntity pm ON pm.projectId = p.id AND pm.deletedAt IS NULL
                LEFT JOIN ProjectCategoryMapEntity pcm ON pcm.projectId = p.id
                WHERE p.deletedAt IS NULL
                  AND (:allowedIdsEmpty = true OR p.id IN :allowedProjectIds)
                  AND (:search IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%'))
                                      OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                                      OR LOWER(p.owner) LIKE LOWER(CONCAT('%', :search, '%')))
                  AND (:customerId IS NULL OR p.customerId = :customerId)
                  AND (:state IS NULL OR p.state = :state)
                  AND (:categoryId IS NULL OR pcm.categoryId = :categoryId)
            """)
    Page<ProjectEntity> findPageWithScope(
            @Param("allowedProjectIds") Collection<Long> allowedProjectIds,
            @Param("allowedIdsEmpty") boolean allowedIdsEmpty,
            @Param("search") String search,
            @Param("customerId") Long customerId,
            @Param("state") com.backend.constructor.core.domain.enums.ProjectState state,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    List<ProjectEntity> findByCustomerIdOrderByIdDesc(Long customerId);

    List<ProjectEntity> findByCustomerIdInOrderByIdDesc(Collection<Long> customerIds);

    boolean existsByCustomerId(Long customerId);
}
