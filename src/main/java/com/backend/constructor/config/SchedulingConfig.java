package com.backend.constructor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật tác vụ định kỳ (2.26: quét & cảnh báo công việc trễ hạn).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
