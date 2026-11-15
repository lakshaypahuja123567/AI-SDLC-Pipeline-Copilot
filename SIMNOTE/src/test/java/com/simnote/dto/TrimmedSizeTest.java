package com.simnote.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TrimmedSizeTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void defaultMinimumRejectsEmptyAfterTrim() {
        assertThat(validator.validate(new Defaults(""))).isNotEmpty();
        assertThat(validator.validate(new Defaults(" \t\n "))).isNotEmpty();
    }

    @Test
    void defaultMinimumAcceptsOneCharacterAfterTrim() {
        assertThat(validator.validate(new Defaults(" a "))).isEmpty();
    }

    @Test
    void nullIsRejected() {
        assertThat(validator.validate(new Defaults(null))).isNotEmpty();
    }

    @Test
    void explicitMinimumIsEnforcedOnTrimmedValue() {
        assertThat(validator.validate(new Bounded("  ab  "))).isNotEmpty();
        assertThat(validator.validate(new Bounded("  abc  "))).isEmpty();
    }

    private record Defaults(@TrimmedSize String value) {
    }

    private record Bounded(@TrimmedSize(min = 3, max = 5) String value) {
    }
}
