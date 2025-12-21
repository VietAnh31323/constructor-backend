package com.backend.constructor.core.service;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HelperService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    private HelperService() {
    }

    public static String joinName(String... parts) {
        return StringUtils.joinWith(" ", (Object[]) parts);
    }

    public static <K, V> V getData(Map<K, V> map, K id) {
        if (Objects.isNull(id) || Objects.isNull(map)) return null;
        return map.get(id);
    }

    public static <T> void addIfNotNull(Set<T> ids, T id) {
        if (Objects.nonNull(id)) ids.add(id);
    }


    public static String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int otp = RANDOM.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
}

