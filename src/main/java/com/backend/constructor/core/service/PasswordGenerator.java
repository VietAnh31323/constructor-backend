package com.backend.constructor.core.service;

import java.security.SecureRandom;

public final class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGIT = "0123456789";
    private static final String SPECIAL = "@#$%&*!?";

    private static final String ALL = LOWER + UPPER + DIGIT + SPECIAL;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generateDefaultPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8");
        }

        StringBuilder password = new StringBuilder(length);

        // Bắt buộc có đủ 4 loại ký tự
        password.append(randomChar(LOWER));
        password.append(randomChar(UPPER));
        password.append(randomChar(DIGIT));
        password.append(randomChar(SPECIAL));

        // Random các ký tự còn lại
        for (int i = 4; i < length; i++) {
            password.append(randomChar(ALL));
        }

        // Xáo trộn thứ tự để tránh predictable
        return shuffle(password.toString());
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }

    private static String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
