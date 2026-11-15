package com.simnote.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simnote.auth.AppUser;
import com.simnote.auth.AppUserRepository;
import com.simnote.dto.NoteRequest;
import com.simnote.dto.NoteResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class NoteServiceTest {

    private static final Long SESSION_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private NoteRepository notes;
    private AppUserRepository users;
    private NoteService service;
    private AppUser sessionUser;

    @BeforeEach
    void setUp() {
        notes = mock(NoteRepository.class);
        users = mock(AppUserRepository.class);
        service = new NoteService(notes, users);
        sessionUser = new AppUser("alice", "hash");
        ReflectionTestUtils.setField(sessionUser, "id", SESSION_USER_ID);
        when(users.findByUsername("alice")).thenReturn(Optional.of(sessionUser));
    }

    @Test
    void listUsesSessionUserIdAndKeepsRepositoryOrder() {
        Note newer = note(sessionUser, "newer", Instant.parse("2026-02-01T00:00:00Z"));
        Note older = note(sessionUser, "older", Instant.parse("2026-01-01T00:00:00Z"));
        when(notes.findAllByUserIdOrderByUpdatedAtDescIdDesc(SESSION_USER_ID)).thenReturn(List.of(newer, older));

        List<NoteResponse> result = service.list("alice");

        verify(notes).findAllByUserIdOrderByUpdatedAtDescIdDesc(SESSION_USER_ID);
        assertThat(result).extracting(NoteResponse::title).containsExactly("newer", "older");
    }

    @Test
    void createSavesNoteOwnedBySessionUser() {
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse result = service.create("alice", new NoteRequest("title", "body"));

        ArgumentCaptor<Note> saved = ArgumentCaptor.forClass(Note.class);
        verify(notes).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("title");
        assertThat(saved.getValue().getContent()).isEqualTo("body");
        assertThat(result.title()).isEqualTo("title");
    }

    @Test
    void createAssignsSessionUserToNote() {
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create("alice", new NoteRequest("title", "body"));

        ArgumentCaptor<Note> saved = ArgumentCaptor.forClass(Note.class);
        verify(notes).save(saved.capture());
        AppUser owner = (AppUser) ReflectionTestUtils.getField(saved.getValue(), "user");
        assertThat(owner).isSameAs(sessionUser);
        assertThat(owner.getId()).isEqualTo(SESSION_USER_ID);
    }

    @Test
    void getQueriesByNoteIdAndSessionUserId() {
        Note owned = note(sessionUser, "mine", Instant.now());
        when(notes.findByIdAndUserId(5L, SESSION_USER_ID)).thenReturn(Optional.of(owned));

        assertThat(service.get("alice", 5L).title()).isEqualTo("mine");
        verify(notes).findByIdAndUserId(5L, SESSION_USER_ID);
    }

    @Test
    void getOtherUsersNoteReturnsNotFound() {
        when(notes.findByIdAndUserId(9L, SESSION_USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.get("alice", 9L));
    }

    @Test
    void updateOtherUsersNoteReturnsNotFoundAndDoesNotSave() {
        when(notes.findByIdAndUserId(9L, SESSION_USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.update("alice", 9L, new NoteRequest("x", "y")));
        verify(notes, never()).save(any());
    }

    @Test
    void deleteOtherUsersNoteReturnsNotFoundAndDoesNotDelete() {
        when(notes.findByIdAndUserId(9L, SESSION_USER_ID)).thenReturn(Optional.empty());

        assertNotFound(() -> service.delete("alice", 9L));
        verify(notes, never()).delete(any());
    }

    @Test
    void deleteOwnNoteRemovesItFromRepository() {
        Note owned = note(sessionUser, "mine", Instant.now());
        when(notes.findByIdAndUserId(5L, SESSION_USER_ID)).thenReturn(Optional.of(owned));

        service.delete("alice", 5L);

        verify(notes).delete(owned);
    }

    private static Note note(AppUser user, String title, Instant updatedAt) {
        return new Note(user, title, "", updatedAt);
    }

    private static void assertNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
