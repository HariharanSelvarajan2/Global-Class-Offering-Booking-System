package com.undoschool.bookingservice.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "BOOKING_CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ApiError> handleBadRequest(BadRequestException exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "BAD_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> handleValidation(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDATION_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(FeignException.NotFound.class)
    ResponseEntity<ApiError> handleUpstreamNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "OFFERING_NOT_FOUND", "Offering was not found."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDatabaseConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "BOOKING_CONFLICT", "Booking could not be confirmed because of a conflicting request."));
    }
}
