package com.simnote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UsernameNormalizerTest {

    @Test
    void trimsAndLowercases() {
        assertThat(UsernameNormalizer.normalize("  Alice_1 ")).isEqualTo("alice_1");
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> UsernameNormalizer.normalize("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> UsernameNormalizer.normalize(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
