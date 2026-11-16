package com.simnote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars",
        "spring.datasource.url=jdbc:h2:mem:simnotetest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class SimNoteFlowTest {

    private static final String JSON = "application/json";

    @Autowired
    private MockMvc mvc;

    @Test
    void authNotesOwnershipAndRememberMeRevocation() throws Exception {
        register("alice");
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"alice\",\"password\":\"password1\"}"))
                .andExpect(status().isConflict());
        register("bob");

        mvc.perform(post("/api/auth/login").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult aliceLogin = login("alice", "password1");
        MockHttpSession aliceSession = (MockHttpSession) aliceLogin.getRequest().getSession(false);
        String rememberHeader = aliceLogin.getResponse().getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith("remember-me="))
                .findFirst()
                .orElseThrow();
        assertThat(rememberHeader).contains("HttpOnly").contains("SameSite=Lax");
        Cookie remember = new Cookie("remember-me",
                rememberHeader.substring("remember-me=".length(), rememberHeader.indexOf(';')));

        MvcResult created = mvc.perform(post("/api/notes").session(aliceSession).with(csrf())
                        .contentType(JSON).content("{\"title\":\"First\",\"content\":\"Body\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long noteId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/notes/" + noteId).session(aliceSession))
                .andExpect(status().isOk());

        MvcResult bobLogin = login("bob", "password1");
        mvc.perform(get("/api/notes/" + noteId).session((MockHttpSession) bobLogin.getRequest().getSession(false)))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/logout").session(aliceSession).with(csrf()).cookie(remember))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notes").cookie(remember))
                .andExpect(status().isUnauthorized());
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated());
    }

    private MvcResult login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }
}
