package com.undoschool.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.undoschool.bookingservice.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class TimeZoneValidatorTest {

    private final TimeZoneValidator validator = new TimeZoneValidator();

    @Test
    void acceptsIanaTimezone() {
        assertThatCode(() -> validator.validate("America/New_York"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownTimezone() {
        assertThatThrownBy(() -> validator.validate("New York"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");
    }
}
