package com.simnote.dto;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record ApiErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ApiErrorResponse of(HttpStatusCode status, String message, String path) {
        return new ApiErrorResponse(Instant.now(), status.value(), reasonOf(status), message, path);
    }

    public static String reasonOf(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }
}
