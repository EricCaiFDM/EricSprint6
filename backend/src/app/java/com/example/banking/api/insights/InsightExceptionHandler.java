package com.example.banking.api.insights;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.banking.api.common.ApiErrorResponse;

@RestControllerAdvice(assignableTypes = SpendingInsightController.class)
public class InsightExceptionHandler {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessFailure(DataAccessException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        "INSIGHT_DEPENDENCY_FAILURE",
                        "Insights could not be generated because a dependency is unavailable",
                        null));
    }
}
