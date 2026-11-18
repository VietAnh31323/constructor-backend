package com.backend.constructor.infras.repository.jpa;

import com.backend.constructor.common.base.repository.BaseJpaRepository;
import com.backend.constructor.core.domain.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationJpaRepository extends BaseJpaRepository<NotificationEntity> {
    Page<NotificationEntity> findAllByRecipientStaffIdOrderByCreatedAtDescIdDesc(Long recipientStaffId, Pageable pageable);

    Page<NotificationEntity> findAllByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDescIdDesc(Long recipientStaffId, Pageable pageable);

    long countByRecipientStaffIdAndIsReadFalse(Long recipientStaffId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now WHERE n.recipientStaffId = :staffId AND n.isRead = false")
    int markAllRead(@Param("staffId") Long staffId, @Param("now") LocalDateTime now);
}
