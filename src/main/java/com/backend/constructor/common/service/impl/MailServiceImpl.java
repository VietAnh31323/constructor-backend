package com.backend.constructor.common.service.impl;

import com.backend.constructor.common.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Async
    @Override
    public void sendPasswordEmail(String toEmail, String password, String role) {
        if (!StringUtils.hasText(mailUsername)) {
            log.warn("SMTP credentials are not configured. Skipping password email to {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setFrom(mailUsername);
            helper.setTo(toEmail);
            helper.setSubject("Gửi mật khẩu từ hệ thống Constructor");

            Context context = new Context();
            context.setVariable("password", password);
            context.setVariable("role", role);

            String htmlContent = templateEngine.process("email/password-email.html", context);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Send password email to {} success", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendOtp(String toEmail, String otp) {
        if (!StringUtils.hasText(mailUsername)) {
            log.warn("SMTP credentials are not configured. Skipping OTP email to {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setFrom(mailUsername);
            helper.setTo(toEmail);
            helper.setSubject("Mã xác thực đặt lại mật khẩu từ hệ thống Constructor");

            Context context = new Context();
            context.setVariable("otp", otp);

            String htmlContent = templateEngine.process("email/reset-password-email.html", context);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Send OTP email to {} success", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
}
