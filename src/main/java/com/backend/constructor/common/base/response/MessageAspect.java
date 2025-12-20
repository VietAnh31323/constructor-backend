package com.backend.constructor.common.base.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MessageAspect {
    @Around("@annotation(message)")
    public Object handleMessage(
            ProceedingJoinPoint joinPoint,
            Message message
    ) throws Throwable {

        try {
            // 1️⃣ Cho phép controller/service chạy
            Object result = joinPoint.proceed();

            // 2️⃣ Nếu không exception → set message
            MessageResponseContext.setResourceMessage(message.value());

            return result;
        } finally {
            log.info("MessageAspect handleMessage");
        }
    }
}
