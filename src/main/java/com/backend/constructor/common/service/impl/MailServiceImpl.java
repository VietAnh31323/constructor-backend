package com.backend.constructor.common.service.impl;

import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.common.service.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    @Override
    public void sendPasswordEmail(String toEmail, String password, String role) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Gửi mật khẩu từ hệ thống constructor");

            // 1. Setup Thymeleaf Context
            Context context = new Context();
            context.setVariable("password", password);
            context.setVariable("role", role);

            // 2. Process template to String
            String htmlContent = templateEngine.process("email/password-email.html", context);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Send password email to {} success", toEmail);
        } catch (Exception e) {
            throw new BusinessException("400", "Gửi email thất bại");
        }
    }

    @Async
    @Override
    public void sendOtp(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Gửi mật khẩu từ hệ thống constructor");

            // 1. Setup Thymeleaf Context
            Context context = new Context();
            context.setVariable("otp", otp);

            // 2. Process template to String
            String htmlContent = templateEngine.process("email/reset-password-email.html", context);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Send OTP email to {} success", toEmail);
        } catch (Exception e) {
            throw new BusinessException("400", "Gửi OTP email thất bại");
        }
    }
}
