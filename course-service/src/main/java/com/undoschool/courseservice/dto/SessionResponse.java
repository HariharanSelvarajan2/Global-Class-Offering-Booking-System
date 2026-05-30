package com.undoschool.courseservice.dto;

import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID offeringId,
        UUID teacherId,
        String startAtUtc,
        String endAtUtc,
        String localStart,
        String localEnd,
        String displayTimezone
) {
}
