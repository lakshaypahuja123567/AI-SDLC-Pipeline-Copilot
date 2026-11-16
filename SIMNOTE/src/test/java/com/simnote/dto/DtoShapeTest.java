package com.simnote.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DtoShapeTest {

    @Test
    void requestAndResponseComponentsMatchSpecification() {
        assertThat(names(RegisterRequest.class)).containsExactly("username", "password");
        assertThat(names(LoginRequest.class)).containsExactly("username", "password");
        assertThat(names(NoteRequest.class)).containsExactly("title", "content");
        assertThat(names(UserResponse.class)).containsExactly("username");
        assertThat(names(NoteResponse.class)).containsExactly("id", "title", "content", "createdAt", "updatedAt");
        assertThat(names(ApiErrorResponse.class)).containsExactly("timestamp", "status", "error", "message", "path");
    }

    @Test
    void responsesContainNoPasswordOrHashField() {
        for (Class<?> type : List.of(UserResponse.class, NoteResponse.class, ApiErrorResponse.class)) {
            assertThat(names(type)).noneMatch(n -> n.toLowerCase().contains("password") || n.toLowerCase().contains("hash"));
        }
    }

    private static List<String> names(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
