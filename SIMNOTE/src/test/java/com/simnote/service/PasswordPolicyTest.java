package com.simnote.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsEightCharactersWithLetterAndDigit() {
        assertThat(PasswordPolicy.isSatisfiedBy("abcdef12")).isTrue();
    }

    @Test
    void rejectsSevenCharactersWithLetterAndDigit() {
        assertThat(PasswordPolicy.isSatisfiedBy("abcde12")).isFalse();
    }

    @Test
    void rejectsLetterOnly() {
        assertThat(PasswordPolicy.isSatisfiedBy("abcdefgh")).isFalse();
    }

    @Test
    void rejectsDigitOnly() {
        assertThat(PasswordPolicy.isSatisfiedBy("12345678")).isFalse();
    }

    @Test
    void rejectsNonAsciiLettersAsLetters() {
        assertThat(PasswordPolicy.isSatisfiedBy("ééééé123")).isFalse();
    }

    @Test
    void acceptsSeventyTwoBytesAndRejectsSeventyThree() {
        assertThat(PasswordPolicy.isSatisfiedBy("a".repeat(71) + "1")).isTrue();
        assertThat(PasswordPolicy.isSatisfiedBy("a".repeat(72) + "1")).isFalse();
    }

    @Test
    void rejectsNull() {
        assertThat(PasswordPolicy.isSatisfiedBy(null)).isFalse();
    }
}
