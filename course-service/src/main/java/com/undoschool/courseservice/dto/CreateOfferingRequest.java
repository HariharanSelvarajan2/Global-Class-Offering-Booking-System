package com.undoschool.courseservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOfferingRequest(
        @NotNull UUID teacherId,
        @NotBlank String courseName,
        @NotBlank String offeringName,
        @NotBlank String teacherTimezone
) {
}
