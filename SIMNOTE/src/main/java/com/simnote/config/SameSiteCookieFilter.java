package com.simnote.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SameSiteCookieFilter extends OncePerRequestFilter {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, new HttpServletResponseWrapper(response) {
            @Override
            public void addCookie(Cookie cookie) {
                ResponseCookie header = ResponseCookie.from(cookie.getName(), cookie.getValue())
                        .path(cookie.getPath() == null ? "/" : cookie.getPath())
                        .maxAge(Duration.ofSeconds(cookie.getMaxAge()))
                        .httpOnly(!CSRF_COOKIE.equals(cookie.getName()))
                        .secure(cookie.getSecure())
                        .sameSite("Lax")
                        .build();
                addHeader(HttpHeaders.SET_COOKIE, header.toString());
            }
        });
    }
}
