package com.undoschool.courseservice.service;

import com.undoschool.courseservice.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TimeZoneService {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.US);
    private static final ZoneId UTC = ZoneId.of("UTC");

    public ZoneId parse(String zone) {
        try {
            return ZoneId.of(zone);
        } catch (Exception exception) {
            throw new BadRequestException("Invalid timezone: " + zone);
        }
    }

    public Instant toInstant(LocalDateTime localDateTime, String zone) {
        return ZonedDateTime.of(localDateTime, parse(zone)).toInstant();
    }

    public String formatUtc(Instant instant) {
        return DISPLAY_FORMATTER.format(instant.atZone(UTC));
    }

    public String formatLocal(Instant instant, String zone) {
        return DISPLAY_FORMATTER.format(instant.atZone(parse(zone)));
    }
}
