package com.example.banking.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.banking.api.insights.InsightExceptionHandler;
import com.example.banking.api.statements.StatementExceptionHandler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;

class ApiAndStatementExceptionHandlerTest {

    @Test
    void apiExceptionHandlerMapsApiErrorAndValidationCases() throws Exception {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ApiErrorException explicit = new ApiErrorException(HttpStatus.CONFLICT, "X_CODE", "boom", "field");
        var explicitResponse = handler.handleApiError(explicit);
        assertEquals(HttpStatus.CONFLICT, explicitResponse.getStatusCode());
        assertEquals("X_CODE", explicitResponse.getBody().code());
        assertEquals("field", explicitResponse.getBody().field());

        MethodArgumentNotValidException withField = validationException("sample", "is required");
        var validationResponse = handler.handleValidationError(withField);
        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals("VALIDATION_ERROR", validationResponse.getBody().code());
        assertEquals("sample", validationResponse.getBody().field());

        MethodArgumentNotValidException withoutField = validationException(null, null);
        var noFieldResponse = handler.handleValidationError(withoutField);
        assertEquals(HttpStatus.BAD_REQUEST, noFieldResponse.getStatusCode());
        assertEquals("VALIDATION_ERROR", noFieldResponse.getBody().code());
        assertNull(noFieldResponse.getBody().field());
        assertEquals("Validation failed", noFieldResponse.getBody().message());

        MethodArgumentTypeMismatchException mismatch = new MethodArgumentTypeMismatchException(
                "x",
                Integer.class,
                "page",
                null,
                new IllegalArgumentException("bad"));
        var mismatchResponse = handler.handleTypeMismatch(mismatch);
        assertEquals(HttpStatus.BAD_REQUEST, mismatchResponse.getStatusCode());
        assertEquals("VALIDATION_ERROR", mismatchResponse.getBody().code());

        HttpMessageNotReadableException malformed = new HttpMessageNotReadableException("bad", (org.springframework.http.HttpInputMessage) null);
        var malformedResponse = handler.handleMalformedPayload(malformed);
        assertEquals(HttpStatus.BAD_REQUEST, malformedResponse.getStatusCode());
        assertEquals("VALIDATION_ERROR", malformedResponse.getBody().code());
    }

    @Test
    void statementExceptionHandlerMapsAllSupportedExceptions() throws Exception {
        StatementExceptionHandler handler = new StatementExceptionHandler();

        MethodArgumentNotValidException withField = validationException("periodYearMonth", "must follow YYYY-MM");
        var validationResponse = handler.handleMethodArgumentNotValid(withField);
        assertEquals(HttpStatus.BAD_REQUEST, validationResponse.getStatusCode());
        assertEquals("STATEMENT_VALIDATION_ERROR", validationResponse.getBody().code());
        assertEquals("periodYearMonth", validationResponse.getBody().field());

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        InvalidStatementQuery invalidQuery = new InvalidStatementQuery(0);
        var violations = validator.validate(invalidQuery);
        ConstraintViolation<InvalidStatementQuery> firstViolation = violations.iterator().next();

        ConstraintViolationException constraint = new ConstraintViolationException(violations);
        var constraintResponse = handler.handleConstraintViolation(constraint);
        assertEquals(HttpStatus.BAD_REQUEST, constraintResponse.getStatusCode());
        assertEquals("STATEMENT_VALIDATION_ERROR", constraintResponse.getBody().code());
        assertEquals("pageSize", constraintResponse.getBody().field());
        assertEquals(firstViolation.getMessage(), constraintResponse.getBody().message());

        MethodArgumentTypeMismatchException mismatch = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "artifactVersion",
                null,
                new IllegalArgumentException("bad"));
        var mismatchResponse = handler.handleTypeMismatch(mismatch);
        assertEquals(HttpStatus.BAD_REQUEST, mismatchResponse.getStatusCode());
        assertEquals("artifactVersion", mismatchResponse.getBody().field());

        HttpMessageNotReadableException malformed = new HttpMessageNotReadableException("bad", (org.springframework.http.HttpInputMessage) null);
        var malformedResponse = handler.handleMalformedPayload(malformed);
        assertEquals(HttpStatus.BAD_REQUEST, malformedResponse.getStatusCode());
        assertEquals("STATEMENT_VALIDATION_ERROR", malformedResponse.getBody().code());

        DateTimeParseException parseException = new DateTimeParseException("bad", "2026-14", 0);
        var dateResponse = handler.handleDateTimeParse(parseException);
        assertEquals(HttpStatus.BAD_REQUEST, dateResponse.getStatusCode());
        assertEquals("periodYearMonth", dateResponse.getBody().field());
    }

    @Test
    void insightExceptionHandlerMapsDataAccessFailures() {
        InsightExceptionHandler handler = new InsightExceptionHandler();

        var response = handler.handleDataAccessFailure(new DataAccessResourceFailureException("db unavailable"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("INSIGHT_DEPENDENCY_FAILURE", response.getBody().code());
        assertNull(response.getBody().field());
    }

    private MethodArgumentNotValidException validationException(String field, String message) throws Exception {
        Method method = ApiAndStatementExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        if (field != null) {
            bindingResult.addError(new FieldError("request", field, message));
        }
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private void validationTarget(String value) {
    }

    private record InvalidStatementQuery(@Min(1) int pageSize) {
    }
}
