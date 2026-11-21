package com.backend.constructor.core.service;

import com.backend.constructor.core.domain.entity.NotificationEntity;
import com.backend.constructor.core.domain.entity.StaffEntity;
import com.backend.constructor.infras.repository.jpa.NotificationJpaRepository;
import com.backend.constructor.infras.repository.jpa.StaffJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final StaffJpaRepository staffJpaRepository;
    private final NotificationJpaRepository notificationJpaRepository;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendForNotifications(List<NotificationEntity> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || StringUtils.isBlank(mailFrom)) {
            log.debug("[EmailNotification] SMTP chưa cấu hình (spring.mail.username trống), bỏ qua gửi email cho {} thông báo", notifications.size());
            return;
        }

        Set<Long> staffIds = notifications.stream()
                .map(NotificationEntity::getRecipientStaffId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, StaffEntity> staffMap = staffJpaRepository.findAllById(staffIds).stream()
                .collect(Collectors.toMap(StaffEntity::getId, Function.identity()));

        List<NotificationEntity> sentEntities = new ArrayList<>();
        for (NotificationEntity n : notifications) {
            StaffEntity staff = staffMap.get(n.getRecipientStaffId());
            if (staff == null || StringUtils.isBlank(staff.getEmail())) {
                continue;
            }
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(mailFrom);
                msg.setTo(staff.getEmail().trim());
                msg.setSubject("[Constructor] " + n.getTitle());
                StringBuilder body = new StringBuilder();
                body.append("Xin chào ").append(StringUtils.defaultIfBlank(staff.getName(), "Anh/Chị")).append(",\n\n");
                body.append(n.getTitle()).append("\n");
                if (StringUtils.isNotBlank(n.getContent())) {
                    body.append(n.getContent()).append("\n");
                }
                if (StringUtils.isNotBlank(n.getLink())) {
                    body.append("\nXem chi tiết tại: ").append(frontendUrl).append(n.getLink()).append("\n");
                }
                body.append("\nTrân trọng,\nHệ thống Quản lý Thi công Constructor");
                msg.setText(body.toString());

                mailSender.send(msg);
                n.setEmailSent(Boolean.TRUE);
                n.setEmailSentAt(LocalDateTime.now());
                sentEntities.add(n);
            } catch (Exception ex) {
                log.warn("[EmailNotification] Gửi email thất bại tới {}: {}", staff.getEmail(), ex.getMessage());
            }
        }
        if (!sentEntities.isEmpty()) {
            notificationJpaRepository.saveAll(sentEntities);
        }
    }
}
