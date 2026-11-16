package com.simnote.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * NFR-008: the session cookie shall be HttpOnly with SameSite=Lax.
 * Verifies the attributes applied to a JSESSIONID Set-Cookie header by {@link SameSiteCookieFilter}.
 */
class SessionCookieAttributesTest {

    @Test
    void sessionCookieIsHttpOnlyWithSameSiteLax() throws Exception {
        SameSiteCookieFilter filter = new SameSiteCookieFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (request, res) -> {
            Cookie session = new Cookie("JSESSIONID", "session-value");
            session.setPath("/");
            ((HttpServletResponse) res).addCookie(session);
        };

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        String header = response.getHeader("Set-Cookie");
        assertThat(header).startsWith("JSESSIONID=session-value");
        assertThat(header).contains("HttpOnly");
        assertThat(header).contains("SameSite=Lax");
    }
}
