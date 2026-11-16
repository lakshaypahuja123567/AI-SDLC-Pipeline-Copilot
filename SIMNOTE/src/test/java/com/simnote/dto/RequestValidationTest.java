package com.simnote.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankTitle() {
        assertThat(validator.validate(new NoteRequest("", "body"))).isNotEmpty();
    }

    @Test
    void rejectsWhitespaceOnlyTitle() {
        assertThat(validator.validate(new NoteRequest("   \t ", "body"))).isNotEmpty();
    }

    @Test
    void rejectsNullTitle() {
        assertThat(validator.validate(new NoteRequest(null, "body"))).isNotEmpty();
    }

    @Test
    void acceptsTitleWithSurroundingWhitespace() {
        assertThat(validator.validate(new NoteRequest("  x  ", null))).isEmpty();
    }

    @Test
    void trimmedSizeEnforcesMaximumOnTrimmedValue() {
        assertThat(validator.validate(new Bounded("  abc  "))).isEmpty();
        assertThat(validator.validate(new Bounded("abcd"))).isNotEmpty();
    }

    @Test
    void titleLengthBoundary() {
        assertThat(validator.validate(new NoteRequest("a".repeat(255), null))).isEmpty();
        assertThat(validator.validate(new NoteRequest("a".repeat(256), null))).isNotEmpty();
    }

    @Test
    void contentLengthBoundary() {
        assertThat(validator.validate(new NoteRequest("x", "a".repeat(10000)))).isEmpty();
        assertThat(validator.validate(new NoteRequest("x", "a".repeat(10001)))).isNotEmpty();
    }

    @Test
    void usernameBoundariesAfterTrim() {
        assertThat(validator.validate(new RegisterRequest("ab", "abcdef12"))).isNotEmpty();
        assertThat(validator.validate(new RegisterRequest(" abc ", "abcdef12"))).isEmpty();
        assertThat(validator.validate(new RegisterRequest("a".repeat(32), "abcdef12"))).isEmpty();
        assertThat(validator.validate(new RegisterRequest("a".repeat(33), "abcdef12"))).isNotEmpty();
    }

    @Test
    void usernameRejectsDisallowedCharacters() {
        assertThat(validator.validate(new RegisterRequest("bad-name", "abcdef12"))).isNotEmpty();
        assertThat(validator.validate(new RegisterRequest("bad name", "abcdef12"))).isNotEmpty();
        assertThat(validator.validate(new RegisterRequest("caf\u00e9_1", "abcdef12"))).isNotEmpty();
    }

    @Test
    void blankUsernameRejected() {
        assertThat(validator.validate(new RegisterRequest("   ", "abcdef12"))).isNotEmpty();
        assertThat(validator.validate(new LoginRequest("", "x"))).isNotEmpty();
    }

    private record Bounded(@TrimmedSize(max = 3) String value) {
    }
}
