package com.backend.constructor.core.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 2.26: So sánh kế hoạch – thực tế để xác định tình trạng tiến độ.
 */
public final class ScheduleStatusUtil {
    public static final String NOT_SCHEDULED = "NOT_SCHEDULED";
    public static final String ON_TRACK = "ON_TRACK";
    public static final String AT_RISK = "AT_RISK";
    public static final String OVERDUE = "OVERDUE";
    public static final String COMPLETED_ON_TIME = "COMPLETED_ON_TIME";
    public static final String COMPLETED_LATE = "COMPLETED_LATE";

    /** Số ngày còn lại tới hạn mà % tiến độ còn thấp thì coi là "có nguy cơ trễ". */
    private static final int AT_RISK_WINDOW_DAYS = 3;
    private static final BigDecimal AT_RISK_PERCENT = BigDecimal.valueOf(80);

    private ScheduleStatusUtil() {
    }

    public static String compute(LocalDate plannedEnd,
                                 LocalDate actualEnd,
                                 boolean completed,
                                 BigDecimal progressPercent,
                                 LocalDate today) {
        if (plannedEnd == null) return NOT_SCHEDULED;
        if (completed) {
            return actualEnd != null && actualEnd.isAfter(plannedEnd) ? COMPLETED_LATE : COMPLETED_ON_TIME;
        }
        if (plannedEnd.isBefore(today)) return OVERDUE;
        BigDecimal pct = progressPercent != null ? progressPercent : BigDecimal.ZERO;
        if (!plannedEnd.isAfter(today.plusDays(AT_RISK_WINDOW_DAYS)) && pct.compareTo(AT_RISK_PERCENT) < 0) {
            return AT_RISK;
        }
        return ON_TRACK;
    }

    /** Số ngày trễ so với kế hoạch (0 nếu đúng / sớm hạn). */
    public static long delayDays(LocalDate plannedEnd, LocalDate actualEnd, boolean completed, LocalDate today) {
        if (plannedEnd == null) return 0L;
        LocalDate compareTo = completed ? (actualEnd != null ? actualEnd : plannedEnd) : today;
        long diff = ChronoUnit.DAYS.between(plannedEnd, compareTo);
        return Math.max(0L, diff);
    }
}
