package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ApiExceptionHandlerTest {

    private static final String JSON = "application/json";

    private MockMvc mvc;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        MvcResult result = mvc.perform(post("/test/validate").contentType(JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/test/validate"))
                .andReturn();
        assertSingleErrorShape(result.getResponse().getContentAsString());
    }

    @Test
    void malformedBodyReturns400WithoutEchoingInput() throws Exception {
        MvcResult result = mvc.perform(post("/test/validate").contentType(JSON).content("{\"name\":\"secret-value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertSingleErrorShape(body);
        assertThat(body).doesNotContain("secret-value");
    }

    @Test
    void responseStatusNotFoundReturns404() throws Exception {
        mvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Note not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    void responseStatusConflictReturns409() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    void unauthenticatedExceptionReturns401() throws Exception {
        MvcResult result = mvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/test/unauthorized"))
                .andReturn();
        assertSingleErrorShape(result.getResponse().getContentAsString());
    }

    @Test
    void accessDeniedExceptionReturns403WithoutDetail() throws Exception {
        MvcResult result = mvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertSingleErrorShape(body);
        assertThat(body).doesNotContain("denied-detail");
    }

    @Test
    void unexpectedExceptionReturns500WithoutStackTraceAndLogsError() throws Exception {
        MvcResult result = mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertSingleErrorShape(body);
        assertThat(body).doesNotContain("internal-detail").doesNotContain("Exception").doesNotContain("\tat ");

        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logs.list.get(0).getThrowableProxy()).isNotNull();
    }

    private static void assertSingleErrorShape(String body) throws Exception {
        Map<String, Object> fields = new ObjectMapper().readValue(body, Map.class);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("timestamp", "status", "error", "message", "path");
        assertThat(body).doesNotContain("\tat ").doesNotContain("stackTrace");
    }

    @RestController
    static class FailingController {

        record Body(@NotBlank String name) {
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody Body body) {
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("internal-detail");
        }

        @GetMapping("/test/unauthorized")
        void unauthorized() {
            throw new InsufficientAuthenticationException("unauth-detail");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new AccessDeniedException("denied-detail");
        }
    }
}
