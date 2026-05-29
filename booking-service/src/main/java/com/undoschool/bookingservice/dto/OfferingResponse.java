package com.undoschool.bookingservice.dto;

import java.util.List;
import java.util.UUID;

public record OfferingResponse(
        UUID id,
        UUID teacherId,
        String courseName,
        String offeringName,
        String teacherTimezone,
        String status,
        List<SessionResponse> sessions
) {
}
