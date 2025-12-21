package com.backend.constructor.common.service;

import org.springframework.scheduling.annotation.Async;

public interface MailService {
    @Async
    void sendPasswordEmail(String toEmail,
                           String password,
                           String role);

    void sendOtp(String toEmail,
                 String otp);
}
