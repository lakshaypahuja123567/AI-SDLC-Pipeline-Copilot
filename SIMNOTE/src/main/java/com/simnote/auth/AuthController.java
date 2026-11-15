package com.simnote.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record Credentials(
            @NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9_]+$") String username,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$") String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final PersistentTokenBasedRememberMeServices rememberMe;

    public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          PersistentTokenBasedRememberMeServices rememberMe) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.rememberMe = rememberMe;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody Credentials credentials) {
        String username = credentials.username().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw usernameTaken();
        }
        try {
            users.saveAndFlush(new AppUser(username, passwordEncoder.encode(credentials.password())));
        } catch (DataIntegrityViolationException ex) {
            throw usernameTaken();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", username));
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest credentials,
                                     HttpServletRequest request, HttpServletResponse response) {
        String username = credentials.username().toLowerCase(Locale.ROOT);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, credentials.password()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        rememberMe.loginSuccess(request, response, authentication);
        return Map.of("username", username);
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }

    private static ResponseStatusException usernameTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
}
