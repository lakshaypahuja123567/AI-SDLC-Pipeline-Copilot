package com.simnote.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.simnote.config.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private static final String JSON = "application/json";

    private AppUserRepository users;
    private AuthenticationManager authenticationManager;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        AuthService service = new AuthService(users, passwordEncoder, authenticationManager);
        PersistentTokenBasedRememberMeServices rememberMe = new PersistentTokenBasedRememberMeServices(
                "test-only-remember-me-key-at-least-32-chars", mock(UserDetailsService.class),
                mock(PersistentTokenRepository.class));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service, rememberMe))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        when(passwordEncoder.encode(any())).thenReturn("hashed");
    }

    @Test
    void registerReturns201WithoutLoggingIn() throws Exception {
        when(users.existsByUsernameNormalized("alice")).thenReturn(false);

        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"username\":\"Alice\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void duplicateUsernameIgnoringCaseReturns409() throws Exception {
        when(users.existsByUsernameNormalized("alice")).thenReturn(true);

        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"username\":\"ALICE\",\"password\":\"password1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void failedLoginReturns401WithGenericMessage() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong-pass-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void meReturns401WithoutAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void meReturns200ForAuthenticatedUser() throws Exception {
        mvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "n/a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }
}
