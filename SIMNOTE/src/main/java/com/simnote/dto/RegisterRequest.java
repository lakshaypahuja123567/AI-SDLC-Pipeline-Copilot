package com.simnote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Username is validated after trimming; the service normalizes it and applies PasswordPolicy. */
public record RegisterRequest(
        @NotBlank @Pattern(regexp = "\\s*[A-Za-z0-9_]{3,32}\\s*") String username,
        @NotBlank String password) {
}
