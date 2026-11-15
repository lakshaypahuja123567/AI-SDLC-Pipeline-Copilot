package com.simnote.service;

import java.nio.charset.StandardCharsets;

/** Registration password rule: at least 8 characters, one ASCII letter, one digit, at most 72 bytes (BCrypt limit). */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isSatisfiedBy(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return false;
        }
        boolean hasLetter = password.chars().anyMatch(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'));
        boolean hasDigit = password.chars().anyMatch(c -> c >= '0' && c <= '9');
        return hasLetter && hasDigit;
    }
}
