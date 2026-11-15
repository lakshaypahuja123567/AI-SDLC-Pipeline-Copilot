package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars",
        "spring.datasource.url=jdbc:h2:mem:securityconfigtest;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class SecurityConfigTest {

    private static final String JSON = "application/json";
    private static final String PASSWORD = "password1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void postWithoutCsrfTokenReturnsJson403() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"username\":\"nocsrf\",\"password\":\"password1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unauthenticatedNotesReturnsJson401() throws Exception {
        mvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"hashcheck\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());

        String stored = jdbc.queryForObject(
                "select password_hash from app_users where username_normalized = 'hashcheck'", String.class);
        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void rememberMeRowIsCreatedOnLoginAndRemovedOnLogout() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"rowcheck\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf()).contentType(JSON)
                        .content("{\"username\":\"rowcheck\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(countRememberRows("rowcheck")).isEqualTo(1);

        String header = login.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith("remember-me="))
                .findFirst()
                .orElseThrow();
        Cookie remember = new Cookie("remember-me",
                header.substring("remember-me=".length(), header.indexOf(';')));
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(post("/api/auth/logout").session(session).with(csrf()).cookie(remember))
                .andExpect(status().isNoContent());
        assertThat(countRememberRows("rowcheck")).isZero();
    }

    private int countRememberRows(String username) {
        Integer count = jdbc.queryForObject(
                "select count(*) from persistent_logins where username = ?", Integer.class, username);
        return count == null ? 0 : count;
    }
}
