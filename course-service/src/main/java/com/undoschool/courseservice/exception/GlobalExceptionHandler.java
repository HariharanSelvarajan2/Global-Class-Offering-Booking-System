package com.undoschool.courseservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "NOT_FOUND", exception.getMessage()));
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
}
