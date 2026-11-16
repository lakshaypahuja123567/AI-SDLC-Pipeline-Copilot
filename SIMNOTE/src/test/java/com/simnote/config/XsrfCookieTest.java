package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "simplenote.remember-me.key=test-only-remember-me-key-at-least-32-chars",
        "spring.datasource.url=jdbc:h2:mem:simnotexsrf;DB_CLOSE_DELAY=-1"})
class XsrfCookieTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void xsrfCookieIsIssuedWithSameSiteLaxAndReadableByScript() {
        ResponseEntity<String> response = rest.getForEntity("/", String.class);

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anySatisfy(cookie -> assertThat(cookie)
                .startsWith("XSRF-TOKEN=")
                .contains("SameSite=Lax")
                .doesNotContain("HttpOnly"));
    }
}
