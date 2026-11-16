package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars",
        "spring.datasource.url=jdbc:h2:mem:simnoteerrorformattest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class ApiErrorFormatTest {

    private static final String JSON = "application/json";

    @Autowired
    private MockMvc mvc;

    @Test
    void unauthenticatedRequestReturns401InErrorFormat() throws Exception {
        MvcResult result = mvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/notes"))
                .andReturn();
        assertSingleErrorShape(result.getResponse().getContentAsString());
    }

    @Test
    void missingCsrfTokenReturns403InErrorFormat() throws Exception {
        MvcResult result = mvc.perform(post("/api/notes").contentType(JSON)
                        .content("{\"title\":\"T\",\"content\":\"C\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/notes"))
                .andReturn();
        assertSingleErrorShape(result.getResponse().getContentAsString());
    }

    @Test
    void unknownRouteReturns404InErrorFormatWithoutSessionSecrets() throws Exception {
        MockHttpSession session = login("erroruser");

        MvcResult result = mvc.perform(get("/api/does-not-exist").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertSingleErrorShape(body);
        assertThat(body).doesNotContain("JSESSIONID").doesNotContain("remember-me").doesNotContain("password");
    }

    private MockHttpSession login(String username) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private static void assertSingleErrorShape(String body) throws Exception {
        Map<String, Object> fields = new ObjectMapper().readValue(body, Map.class);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("timestamp", "status", "error", "message", "path");
        assertThat(body).doesNotContain("\tat ").doesNotContain("stackTrace");
    }
}
