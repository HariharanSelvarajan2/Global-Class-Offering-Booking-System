package com.undoschool.bookingservice.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID bookingId,
        UUID parentId,
        UUID offeringId,
        String courseName,
        String offeringName,
        String status,
        Instant bookedAt,
        OfferingResponse offering
) {
}
