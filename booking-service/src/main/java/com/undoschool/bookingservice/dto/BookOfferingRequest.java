package com.undoschool.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BookOfferingRequest(
        @NotNull UUID parentId,
        @NotNull UUID offeringId,
        @NotBlank String timezone
) {
}
