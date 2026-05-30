package com.undoschool.courseservice.service;

import com.undoschool.courseservice.exception.BadRequestException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class TimeZoneService {

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

    public OffsetDateTime toLocal(Instant instant, String zone) {
        return instant.atZone(parse(zone)).toOffsetDateTime();
    }
}
