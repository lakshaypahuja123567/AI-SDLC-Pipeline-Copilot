package com.simnote.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.simnote.dto.LoginRequest;
import com.simnote.dto.RegisterRequest;
import com.simnote.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTest {

    private AppUserRepository users;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private AuthService service;
    private ListAppender<ILoggingEvent> logEvents;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        service = new AuthService(users, passwordEncoder, authenticationManager);
        logEvents = new ListAppender<>();
        logEvents.start();
        ((Logger) LoggerFactory.getLogger(AuthService.class)).addAppender(logEvents);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(AuthService.class)).detachAppender(logEvents);
    }

    @Test
    void registerStoresNormalizedUsernameWithoutLoggingIn() {
        when(users.existsByUsernameNormalized("alice")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");

        UserResponse result = service.register(new RegisterRequest(" Alice ", "password1"));

        assertThat(result).isEqualTo(new UserResponse("alice"));
        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void registerRejectsDuplicateUsernameCaseInsensitively() {
        when(users.existsByUsernameNormalized("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("ALICE", "password1")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(users).existsByUsernameNormalized("alice");
        verify(users, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "password")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(users);
    }

    @Test
    void loginFailureReturnsGenericMessageAndWarnsWithoutPassword() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(new LoginRequest("Alice", "wrong-pass-1")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.getReason()).isEqualTo("Invalid username or password.");
                });

        assertThat(logEvents.list).hasSize(1);
        assertThat(logEvents.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(logEvents.list.get(0).getFormattedMessage()).doesNotContain("wrong-pass-1");
    }

    @Test
    void loginSuccessReturnsAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "n/a");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        assertThat(service.login(new LoginRequest("Alice", "password1"))).isSameAs(authentication);
    }

    @Test
    void currentUserReturnsUsernameWhenAuthenticated() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "n/a");

        assertThat(service.currentUser(authentication)).isEqualTo(new UserResponse("alice"));
    }

    @Test
    void currentUserRejectsMissingOrAnonymousAuthentication() {
        assertThatThrownBy(() -> service.currentUser(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        assertThatThrownBy(() -> service.currentUser(anonymous))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
