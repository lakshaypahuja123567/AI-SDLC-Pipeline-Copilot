package com.simnote.auth;

import com.simnote.dto.LoginRequest;
import com.simnote.dto.RegisterRequest;
import com.simnote.dto.UserResponse;
import com.simnote.service.PasswordPolicy;
import com.simnote.service.UsernameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS = "Invalid username or password.";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    /** Creates the account without logging the user in. */
    public UserResponse register(RegisterRequest request) {
        String username = UsernameNormalizer.normalize(request.username());
        if (!PasswordPolicy.isSatisfiedBy(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be 8 to 72 bytes and contain a letter and a digit");
        }
        if (users.existsByUsernameNormalized(username)) {
            throw usernameTaken();
        }
        try {
            users.saveAndFlush(new AppUser(username, passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException ex) {
            throw usernameTaken();
        }
        return new UserResponse(username);
    }

    public Authentication login(LoginRequest request) {
        String username = UsernameNormalizer.normalize(request.username());
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for username {}", username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
    }

    public UserResponse currentUser(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return new UserResponse(authentication.getName());
    }

    private static ResponseStatusException usernameTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
}
