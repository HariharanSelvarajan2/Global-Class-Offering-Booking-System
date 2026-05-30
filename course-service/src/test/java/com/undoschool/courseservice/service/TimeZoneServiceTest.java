package com.undoschool.courseservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.undoschool.courseservice.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TimeZoneServiceTest {

    private final TimeZoneService timeZoneService = new TimeZoneService();

    @Test
    void convertsTeacherLocalTimeToUtcInstant() {
        Instant instant = timeZoneService.toInstant(
                LocalDateTime.of(2026, 6, 6, 18, 0),
                "Asia/Kolkata"
        );

        assertThat(instant).isEqualTo(Instant.parse("2026-06-06T12:30:00Z"));
    }

    @Test
    void rejectsInvalidTimezone() {
        assertThatThrownBy(() -> timeZoneService.parse("Not/A_Timezone"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");
    }

    @Test
    void convertsUtcInstantToRequestedLocalOffsetTime() {
        Instant instant = Instant.parse("2026-06-13T17:30:00Z");

        assertThat(timeZoneService.toLocal(instant, "Asia/Kolkata").toString())
                .isEqualTo("2026-06-13T23:00+05:30");
    }
}
