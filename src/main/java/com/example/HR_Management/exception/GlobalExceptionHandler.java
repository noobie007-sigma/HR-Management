package com.example.HR_Management.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse response = new ErrorResponse(
                400, "Bad Request", "Malformed JSON request body", List.of(), uri);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse response = new ErrorResponse(
                400, "Validation Failed", "Invalid request body", errors, uri);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

   
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

    
    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<ErrorResponse> handleConversion(
            HttpMessageConversionException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid input format", List.of(), request);
    }

   
    @ExceptionHandler({
            EntityNotFoundException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                List.of(), request);
    }

   
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "Database Error",
                "Data integrity violation",
                List.of(ex.getMostSpecificCause().getMessage()),
                request);
    }

    
    @ExceptionHandler(RepositoryConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleRepositoryValidation(
            RepositoryConstraintViolationException ex,
            HttpServletRequest request) {

        List<String> errors = ex.getErrors()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.toList());

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Repository validation failed", errors, request);
    }

   
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", ex.getMessage(), List.of(), request);
    }

    
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            List<String> details,
            HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                status.value(), error, message, details, request.getRequestURI());
        return ResponseEntity.status(status).body(response);
    }
}