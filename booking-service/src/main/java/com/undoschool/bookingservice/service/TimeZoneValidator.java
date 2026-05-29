package com.undoschool.bookingservice.service;

import com.undoschool.bookingservice.exception.BadRequestException;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class TimeZoneValidator {

    public void validate(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception exception) {
            throw new BadRequestException("Invalid timezone: " + timezone);
        }
    }
}
