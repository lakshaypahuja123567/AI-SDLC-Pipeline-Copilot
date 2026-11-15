package com.simnote.service;

import java.util.Locale;

/** Trims and lowercases a username to its stored form; blank input is rejected. */
public final class UsernameNormalizer {

    private UsernameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        return raw.strip().toLowerCase(Locale.ROOT);
    }
}
