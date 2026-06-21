package org.example.bookshop.dto.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldError> validationErrors
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError validation(String path, List<FieldError> errors) {
        return new ApiError(Instant.now(), 400, "Bad Request", "Validation failed", path, errors);
    }
}
