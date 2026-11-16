package com.simnote.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.simnote.SimNoteApplication;
import com.simnote.auth.AppUser;
import com.simnote.auth.AppUserRepository;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class NotePersistenceTest {

    @TempDir
    Path dir;

    @Test
    void schemaConstraintsFilteringAndDataSurviveRestart() {
        Long aliceId;
        Long noteId;
        try (ConfigurableApplicationContext ctx = start()) {
            AppUserRepository users = ctx.getBean(AppUserRepository.class);
            NoteRepository notes = ctx.getBean(NoteRepository.class);

            AppUser alice = users.saveAndFlush(new AppUser("Alice", "hash"));
            AppUser bob = users.saveAndFlush(new AppUser("bob", "hash"));
            aliceId = alice.getId();
            noteId = notes.saveAndFlush(new Note(alice, "First", "Body", Instant.now())).getId();
            notes.saveAndFlush(new Note(bob, "Bob note", "Body", Instant.now()));

            assertThatThrownBy(() -> users.saveAndFlush(new AppUser("ALICE", "hash")))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(notes.findByIdAndUserId(noteId, bob.getId())).isEmpty();
            assertThat(notes.findAllByUserIdOrderByUpdatedAtDescIdDesc(aliceId))
                    .extracting(Note::getTitle)
                    .containsExactly("First");
            assertThat(ctx.getBean(JdbcTemplate.class)
                    .queryForObject("select count(*) from persistent_logins", Integer.class)).isZero();
        }

        try (ConfigurableApplicationContext ctx = start()) {
            NoteRepository notes = ctx.getBean(NoteRepository.class);
            assertThat(notes.findByIdAndUserId(noteId, aliceId))
                    .hasValueSatisfying(note -> assertThat(note.getContent()).isEqualTo("Body"));
        }
    }

    private ConfigurableApplicationContext start() {
        String url = "jdbc:h2:file:" + dir.resolve("simplenote").toAbsolutePath().toString().replace('\\', '/');
        return new SpringApplicationBuilder(SimNoteApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + url,
                        "--simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars");
    }
}
