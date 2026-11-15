package com.simnote.auth;

import com.simnote.dto.LoginRequest;
import com.simnote.dto.RegisterRequest;
import com.simnote.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Logout is served by the logout filter configured in SecurityConfig at POST /api/auth/logout (204). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PersistentTokenBasedRememberMeServices rememberMe;

    public AuthController(AuthService authService, PersistentTokenBasedRememberMeServices rememberMe) {
        this.authService = authService;
        this.rememberMe = rememberMe;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest, HttpServletResponse response) {
        Authentication authentication = authService.login(request);
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, response);
        rememberMe.loginSuccess(httpRequest, response, authentication);
        return new UserResponse(authentication.getName());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.currentUser(authentication);
    }
}
