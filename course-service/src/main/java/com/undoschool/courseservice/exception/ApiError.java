package com.undoschool.courseservice.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message
) {
    public static ApiError of(int status, String code, String message) {
        return new ApiError(Instant.now(), status, code, message);
    }
}
