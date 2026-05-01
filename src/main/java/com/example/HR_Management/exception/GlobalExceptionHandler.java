package com.example.HR_Management.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================================
    // 🔴 1. @Valid BODY VALIDATION
    // ================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .collect(Collectors.toList());

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Invalid request body", errors, request);
    }

    // ================================
    // 🔴 2. CONSTRAINT VIOLATION
    // ================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + " : " + v.getMessage())
                .collect(Collectors.toList());

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Constraint violation", errors, request);
    }

    // ================================
    // 🔴 3. INVALID PATH / PARAM TYPE
    // ================================
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            ConversionFailedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid request parameter", List.of(ex.getMessage()), request);
    }

    // ================================
    // 🔴 4. JSON PARSE / FORMAT ERROR
    // ================================
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<ErrorResponse> handleConversion(
            HttpMessageConversionException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid input format", List.of(), request);
    }

    // ================================
    // 🔴 5. NOT FOUND (IMPORTANT)
    // ================================
    @ExceptionHandler({
            EntityNotFoundException.class,
            org.springframework.data.rest.webmvc.ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                List.of(), request);
    }

    // ================================
    // 🔴 6. DATABASE ERRORS
    // ================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "Database Error",
                "Data integrity violation",
                List.of(ex.getMostSpecificCause().getMessage()),
                request);
    }

    // ================================
    // 🔴 7. FALLBACK (NO MORE RANDOM 500s)
    // ================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                ex.getMessage(),
                List.of(),
                request);
    }

    // ================================
    // 🟢 COMMON RESPONSE BUILDER
    // ================================
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            List<String> details,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                status.value(),
                error,
                message,
                details,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }
}