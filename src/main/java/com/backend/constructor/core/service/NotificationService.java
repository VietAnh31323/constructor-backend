package com.backend.constructor.core.service;

import com.backend.constructor.app.dto.notification.NotificationDto;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.NotificationEntity;
import com.backend.constructor.core.domain.enums.NotificationType;
import com.backend.constructor.infras.repository.jpa.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationJpaRepository notificationRepository;
    private final CurrentUserService currentUserService;

    /**
     * Gửi thông báo nội bộ tới danh sách nhân sự (bỏ qua null, trùng lặp và người thực hiện thao tác).
     */
    @Transactional
    public void notifyStaffs(Collection<Long> staffIds,
                             NotificationType type,
                             String title,
                             String content,
                             String link,
                             String refType,
                             Long refId,
                             Long excludeStaffId) {
        if (staffIds == null || staffIds.isEmpty()) return;
        Set<Long> recipients = new LinkedHashSet<>();
        for (Long id : staffIds) {
            if (id != null && !Objects.equals(id, excludeStaffId)) {
                recipients.add(id);
            }
        }
        if (recipients.isEmpty()) return;
        List<NotificationEntity> entities = new ArrayList<>();
        for (Long staffId : recipients) {
            entities.add(NotificationEntity.builder()
                    .recipientStaffId(staffId)
                    .type(type)
                    .title(StringUtils.abbreviate(title, 250))
                    .content(content)
                    .link(StringUtils.abbreviate(link, 495))
                    .refType(refType)
                    .refId(refId)
                    .isRead(Boolean.FALSE)
                    .build());
        }
        notificationRepository.saveAll(entities);
    }

    public Page<NotificationDto> getMyNotifications(boolean unreadOnly, Pageable pageable) {
        Long staffId = requireStaffId();
        Page<NotificationEntity> page = unreadOnly
                ? notificationRepository.findAllByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDescIdDesc(staffId, pageable)
                : notificationRepository.findAllByRecipientStaffIdOrderByCreatedAtDescIdDesc(staffId, pageable);
        return page.map(this::toDto);
    }

    public long countMyUnread() {
        Long staffId = currentUserService.getCurrentStaffId();
        if (staffId == null) return 0L;
        return notificationRepository.countByRecipientStaffIdAndIsReadFalse(staffId);
    }

    @Transactional
    public void markRead(Long id) {
        Long staffId = requireStaffId();
        NotificationEntity entity = notificationRepository.findById(id)
                .orElseThrow(() -> BusinessException.exception("CST002"));
        if (!Objects.equals(entity.getRecipientStaffId(), staffId)) {
            throw BusinessException.exception("CST002");
        }
        if (!Boolean.TRUE.equals(entity.getIsRead())) {
            entity.setIsRead(Boolean.TRUE);
            entity.setReadAt(LocalDateTime.now());
            notificationRepository.save(entity);
        }
    }

    @Transactional
    public int markAllRead() {
        Long staffId = requireStaffId();
        return notificationRepository.markAllRead(staffId, LocalDateTime.now());
    }

    private Long requireStaffId() {
        Long staffId = currentUserService.getCurrentStaffId();
        if (staffId == null) {
            throw BusinessException.exception("CST020");
        }
        return staffId;
    }

    private NotificationDto toDto(NotificationEntity e) {
        return NotificationDto.builder()
                .id(e.getId())
                .type(e.getType())
                .typeLabel(e.getType() != null ? e.getType().getLabel() : null)
                .title(e.getTitle())
                .content(e.getContent())
                .link(e.getLink())
                .refType(e.getRefType())
                .refId(e.getRefId())
                .isRead(e.getIsRead())
                .readAt(e.getReadAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
