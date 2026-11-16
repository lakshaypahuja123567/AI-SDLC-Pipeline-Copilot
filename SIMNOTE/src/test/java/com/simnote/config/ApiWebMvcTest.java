package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simnote.auth.AuthController;
import com.simnote.auth.AuthService;
import com.simnote.dto.NoteRequest;
import com.simnote.dto.NoteResponse;
import com.simnote.dto.RegisterRequest;
import com.simnote.dto.UserResponse;
import com.simnote.note.NoteService;
import com.simnote.note.NotesController;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@WebMvcTest(controllers = {AuthController.class, NotesController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars")
class ApiWebMvcTest {

    private static final String JSON = "application/json";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StubAuthService authService;

    @Autowired
    private StubNoteService noteService;

    @BeforeEach
    void resetStubs() {
        authService.reset();
        noteService.reset();
    }

    /**
     * Hand-written doubles that extend the real services. Mockito cannot mock concrete
     * classes on the JDK 27 runtime (Byte Buddy does not support class file version 71).
     * Interface dependencies are passed as null because these overrides never call them.
     */
    @TestConfiguration
    static class MockedServices {

        @Bean
        StubAuthService authService() {
            return new StubAuthService();
        }

        @Bean
        StubNoteService noteService() {
            return new StubNoteService();
        }

        /**
         * Dynamic proxy instead of Mockito: Mockito cannot instrument JDK interfaces such as
         * DataSource on this runtime. This slice never opens a connection, so any call fails fast.
         */
        @Bean
        DataSource dataSource() {
            return (DataSource) java.lang.reflect.Proxy.newProxyInstance(
                    DataSource.class.getClassLoader(),
                    new Class<?>[] {DataSource.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "equals" -> proxy == args[0];
                                case "hashCode" -> System.identityHashCode(proxy);
                                default -> "DataSourceStub";
                            };
                        }
                        throw new UnsupportedOperationException(
                                "DataSource." + method.getName() + " is not available in WebMvc slice tests");
                    });
        }

        @Bean
        UserDetailsService userDetailsService() {
            return mock(UserDetailsService.class);
        }
    }

    static final class StubAuthService extends AuthService {
        UserResponse registerResult;
        RuntimeException registerError;
        int registerCalls;

        StubAuthService() {
            super(null, null, null);
        }

        @Override
        public UserResponse register(RegisterRequest request) {
            registerCalls++;
            if (registerError != null) {
                throw registerError;
            }
            return registerResult;
        }

        void reset() {
            registerResult = null;
            registerError = null;
            registerCalls = 0;
        }
    }

    static final class StubNoteService extends NoteService {
        NoteResponse createResult;
        RuntimeException getError;
        int createCalls;
        String deletedBy;
        Long deletedId;

        StubNoteService() {
            super(null, null, java.time.Clock.systemUTC());
        }

        @Override
        public NoteResponse create(String username, NoteRequest request) {
            createCalls++;
            return createResult;
        }

        @Override
        public NoteResponse get(String username, Long id) {
            if (getError != null) {
                throw getError;
            }
            return null;
        }

        @Override
        public void delete(String username, Long id) {
            deletedBy = username;
            deletedId = id;
        }

        void reset() {
            createResult = null;
            getError = null;
            createCalls = 0;
            deletedBy = null;
            deletedId = null;
        }
    }

    @Test
    void registerBindsValidBodyAndReturns201WithoutSecrets() throws Exception {
        authService.registerResult = new UserResponse("alice");

        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("password1");
    }

    @Test
    void registerMissingPasswordReturns400ErrorShape() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();

        assertErrorShape(result);
        assertThat(authService.registerCalls).isZero();
    }

    @Test
    void registerDuplicateReturns409ErrorShape() throws Exception {
        authService.registerError = new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");

        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"))
                .andReturn();

        assertErrorShape(result);
    }

    @Test
    @WithMockUser(username = "alice")
    void createNoteBindsValidBodyAndReturns201() throws Exception {
        Instant now = Instant.now();
        noteService.createResult = new NoteResponse(1L, "First", "Body", now, now);

        mvc.perform(post("/api/notes").with(csrf()).contentType(JSON)
                        .content("{\"title\":\"First\",\"content\":\"Body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("First"));
    }

    @Test
    @WithMockUser(username = "alice")
    void createNoteWithoutCsrfReturns403ErrorShape() throws Exception {
        MvcResult result = mvc.perform(post("/api/notes").contentType(JSON)
                        .content("{\"title\":\"First\",\"content\":\"Body\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andReturn();

        assertErrorShape(result);
        assertThat(noteService.createCalls).isZero();
    }

    @Test
    @WithMockUser(username = "alice")
    void createNoteBlankTitleReturns400() throws Exception {
        MvcResult result = mvc.perform(post("/api/notes").with(csrf()).contentType(JSON)
                        .content("{\"title\":\"   \",\"content\":\"Body\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorShape(result);
        assertThat(noteService.createCalls).isZero();
    }

    @Test
    @WithMockUser(username = "alice")
    void createNoteTitleOver255Returns400() throws Exception {
        mvc.perform(post("/api/notes").with(csrf()).contentType(JSON)
                        .content("{\"title\":\"" + "t".repeat(256) + "\",\"content\":\"Body\"}"))
                .andExpect(status().isBadRequest());
        assertThat(noteService.createCalls).isZero();
    }

    @Test
    @WithMockUser(username = "alice")
    void createNoteContentOver10000Returns400() throws Exception {
        mvc.perform(post("/api/notes").with(csrf()).contentType(JSON)
                        .content("{\"title\":\"First\",\"content\":\"" + "c".repeat(10_001) + "\"}"))
                .andExpect(status().isBadRequest());
        assertThat(noteService.createCalls).isZero();
    }

    @Test
    @WithMockUser(username = "alice")
    void getNonNumericIdReturns400ErrorShape() throws Exception {
        MvcResult result = mvc.perform(get("/api/notes/not-a-number"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertErrorShape(result);
    }

    @Test
    @WithMockUser(username = "alice")
    void getMissingNoteReturns404ErrorShape() throws Exception {
        noteService.getError = new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");

        MvcResult result = mvc.perform(get("/api/notes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Note not found"))
                .andReturn();

        assertErrorShape(result);
    }

    @Test
    @WithMockUser(username = "alice")
    void deleteNoteReturns204() throws Exception {
        mvc.perform(delete("/api/notes/5").with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(noteService.deletedBy).isEqualTo("alice");
        assertThat(noteService.deletedId).isEqualTo(5L);
    }

    private static void assertErrorShape(MvcResult result) throws Exception {
        Map<String, Object> fields = new ObjectMapper().readValue(
                result.getResponse().getContentAsString(), Map.class);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("timestamp", "status", "error", "message", "path");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("\tat ").doesNotContain("stackTrace");
    }
}
