package com.backend.constructor.app.dto.notification;

import com.backend.constructor.core.domain.enums.NotificationType;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private NotificationType type;
    private String typeLabel;
    private String title;
    private String content;
    private String link;
    private String refType;
    private Long refId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Instant createdAt;
}
