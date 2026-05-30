package com.undoschool.courseservice.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID offeringId,
        UUID teacherId,
        Instant startAtUtc,
        Instant endAtUtc,
        OffsetDateTime localStart,
        OffsetDateTime localEnd,
        String displayTimezone
) {
}
