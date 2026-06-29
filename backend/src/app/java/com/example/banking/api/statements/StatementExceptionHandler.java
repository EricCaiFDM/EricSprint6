package com.example.banking.api.statements;

import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.banking.api.common.ApiErrorResponse;
import com.example.banking.api.statements.routes.StatementGenerationController;
import com.example.banking.api.statements.routes.StatementQueryController;
import com.example.banking.api.statements.routes.StatementRetrievalController;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice(assignableTypes = {
        StatementGenerationController.class,
        StatementRetrievalController.class,
        StatementQueryController.class
})
public class StatementExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        FieldError firstFieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstFieldError != null ? firstFieldError.getDefaultMessage() : "Validation failed";
        String field = firstFieldError != null ? firstFieldError.getField() : null;

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "STATEMENT_VALIDATION_ERROR",
                        message,
                        field));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Validation failed");
        String field = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath().toString())
                .orElse(null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "STATEMENT_VALIDATION_ERROR",
                        message,
                        field));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "STATEMENT_VALIDATION_ERROR",
                        "Malformed request payload",
                        exception.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedPayload(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "STATEMENT_VALIDATION_ERROR",
                        "Malformed request payload",
                        null));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiErrorResponse> handleDateTimeParse(DateTimeParseException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        "STATEMENT_VALIDATION_ERROR",
                        "periodYearMonth must follow YYYY-MM",
                        "periodYearMonth"));
    }
}
