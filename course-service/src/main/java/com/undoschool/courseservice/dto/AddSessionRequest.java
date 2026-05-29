package com.undoschool.courseservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AddSessionRequest(
        @NotNull LocalDateTime localStart,
        @NotNull LocalDateTime localEnd,
        String timezone
) {
}
