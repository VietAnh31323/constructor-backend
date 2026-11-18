package com.backend.constructor.app.rest;

import com.backend.constructor.app.dto.notification.NotificationDto;
import com.backend.constructor.common.base.response.paging.HandsomePaging;
import com.backend.constructor.core.service.NotificationService;
import com.backend.constructor.core.service.TaskOverdueAlertService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationController {
    private final NotificationService notificationService;
    private final TaskOverdueAlertService taskOverdueAlertService;

    @GetMapping("/my")
    @HandsomePaging
    @PreAuthorize("isAuthenticated()")
    public Page<NotificationDto> getMyNotifications(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                    @ParameterObject Pageable pageable) {
        return notificationService.getMyNotifications(unreadOnly, pageable);
    }

    @GetMapping("/my/unread-count")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> getMyUnreadCount() {
        return Map.of("count", notificationService.countMyUnread());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id);
    }

    @PatchMapping("/my/read-all")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead());
    }

    /** 2.26: Quét & gửi cảnh báo trễ hạn ngay lập tức (ngoài lịch tự động) */
    @PostMapping("/scan-overdue")
    @PreAuthorize("hasAnyAuthority('PROJECT_MANAGER', 'PROGRESS_UPDATE', 'SCOPE_ADMIN')")
    public Map<String, Integer> scanOverdueNow() {
        return Map.of("notifiedTasks", taskOverdueAlertService.scanAndNotify());
    }
}
