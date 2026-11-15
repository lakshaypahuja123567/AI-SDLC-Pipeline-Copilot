package com.simnote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String JSON = "application/json";
    private static final String PASSWORD = "password1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Environment env;

    private ListAppender<ILoggingEvent> logs;
    private Logger rootLogger;

    @BeforeEach
    void attachLogCapture() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logs = new ListAppender<>();
        logs.start();
        rootLogger.addAppender(logs);
    }

    @AfterEach
    void detachLogCapture() {
        rootLogger.detachAppender(logs);
    }

    @Test
    void testProfileUsesInMemoryH2NotDataFile() {
        assertThat(env.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
    }

    @Test
    void noteCrudListUpdateDeleteForOwner() throws Exception {
        MockHttpSession session = registerAndLogin("crudowner");

        long id = createNote(session, "First", "Body");

        mvc.perform(get("/api/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].title").value("First"));

        mvc.perform(put("/api/notes/" + id).session(session).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"Edited\",\"content\":\"New body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited"))
                .andExpect(jsonPath("$.content").value("New body"));

        mvc.perform(get("/api/notes/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited"));

        mvc.perform(delete("/api/notes/" + id).session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/notes/" + id).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUsersCannotUpdateOrDeleteNote() throws Exception {
        MockHttpSession alice = registerAndLogin("alicex");
        MockHttpSession bob = registerAndLogin("bobx");
        long id = createNote(alice, "Private", "Body");

        mvc.perform(put("/api/notes/" + id).session(bob).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"Hijack\",\"content\":\"x\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/notes/" + id).session(bob).with(csrf()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/notes/" + id).session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Private"));
    }

    @Test
    void invalidInputReturns400ThroughFullStack() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"ab\",\"password\":\"password1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"shortpw\",\"password\":\"short1\"}"))
                .andExpect(status().isBadRequest());

        MockHttpSession session = registerAndLogin("validator");
        mvc.perform(post("/api/notes").session(session).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"   \",\"content\":\"Body\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notes").session(session).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"" + "t".repeat(256) + "\",\"content\":\"Body\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notes").session(session).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"ok\",\"content\":\"" + "c".repeat(10_001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responseBodiesNeverContainSecretsSessionIdOrStackTrace() throws Exception {
        MvcResult register = mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"nosecrets\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        assertNoSecrets(register.getResponse().getContentAsString());

        MvcResult login = login("nosecrets", PASSWORD);
        String loginBody = login.getResponse().getContentAsString();
        assertNoSecrets(loginBody);
        assertThat(loginBody).doesNotContain(login.getRequest().getSession(false).getId());

        MvcResult unauthorized = mvc.perform(get("/api/notes")).andExpect(status().isUnauthorized()).andReturn();
        assertNoSecrets(unauthorized.getResponse().getContentAsString());

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        MvcResult notFound = mvc.perform(get("/api/notes/424242").session(session))
                .andExpect(status().isNotFound())
                .andReturn();
        assertNoSecrets(notFound.getResponse().getContentAsString());

        MvcResult badRequest = mvc.perform(post("/api/notes").session(session).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"   \",\"content\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertNoSecrets(badRequest.getResponse().getContentAsString());
    }

    @Test
    void h2ConsoleIsNotServed() throws Exception {
        mvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = registerAndLogin("consolecheck");
        mvc.perform(get("/h2-console/").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordsAreNotWrittenToLogs() throws Exception {
        String password = "LogSecret42";
        String wrong = "WrongSecret42";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"logcheck\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"logcheck\",\"password\":\"" + wrong + "\"}"))
                .andExpect(status().isUnauthorized());
        login("logcheck", password);

        assertThat(logs.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("logcheck");
                });
        for (ILoggingEvent event : logs.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(password).doesNotContain(wrong);
        }
    }

    private MockHttpSession registerAndLogin(String username) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return (MockHttpSession) login(username, PASSWORD).getRequest().getSession(false);
    }

    private MvcResult login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private long createNote(MockHttpSession session, String title, String content) throws Exception {
        MvcResult created = mvc.perform(post("/api/notes").session(session).with(csrf()).contentType(JSON)
                        .content("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static void assertNoSecrets(String body) {
        assertThat(body)
                .doesNotContain(PASSWORD)
                .doesNotContain("$2a$")
                .doesNotContain("$2b$")
                .doesNotContain("password")
                .doesNotContain("JSESSIONID")
                .doesNotContain("Exception")
                .doesNotContain("\tat ")
                .doesNotContain("stackTrace");
    }
}
