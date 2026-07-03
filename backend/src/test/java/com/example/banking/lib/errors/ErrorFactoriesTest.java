package com.example.banking.lib.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

class ErrorFactoriesTest {

    @Test
    void customerErrorsExposeExpectedContracts() {
        ApiErrorException validation = CustomerErrors.validation("bad", "field");
        assertEquals(HttpStatus.BAD_REQUEST, validation.getStatus());
        assertEquals("CUSTOMER_VALIDATION_ERROR", validation.getCode());
        assertEquals("field", validation.getField());

        ApiErrorException notFound = CustomerErrors.notFound();
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
        assertEquals("CUSTOMER_NOT_FOUND", notFound.getCode());
        assertEquals("customerId", notFound.getField());

        ApiErrorException forbidden = CustomerErrors.forbidden("read");
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertEquals("CUSTOMER_FORBIDDEN", forbidden.getCode());
        assertNull(forbidden.getField());

        ApiErrorException conflict = CustomerErrors.conflict("dup", "email");
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
        assertEquals("CUSTOMER_CONFLICT", conflict.getCode());
        assertEquals("email", conflict.getField());

        ApiErrorException blocked = CustomerErrors.deleteBlocked();
        assertEquals(HttpStatus.CONFLICT, blocked.getStatus());
        assertEquals("CUSTOMER_DELETE_BLOCKED", blocked.getCode());
    }

    @Test
    void accountErrorsExposeExpectedContracts() {
        ApiErrorException validation = AccountErrors.validation("bad", "field");
        assertEquals(HttpStatus.BAD_REQUEST, validation.getStatus());
        assertEquals("ACCOUNT_VALIDATION_ERROR", validation.getCode());

        ApiErrorException notFound = AccountErrors.notFound();
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
        assertEquals("ACCOUNT_NOT_FOUND", notFound.getCode());

        ApiErrorException forbidden = AccountErrors.forbidden("delete");
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertEquals("ACCOUNT_FORBIDDEN", forbidden.getCode());

        ApiErrorException conflict = AccountErrors.conflict("dup", "nickname");
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
        assertEquals("ACCOUNT_CONFLICT", conflict.getCode());

        ApiErrorException blocked = AccountErrors.deleteBlocked();
        assertEquals(HttpStatus.CONFLICT, blocked.getStatus());
        assertEquals("ACCOUNT_DELETE_BLOCKED", blocked.getCode());
    }

    @Test
    void standingOrderErrorsExposeExpectedContracts() {
        ApiErrorException validation = StandingOrderErrors.validation("bad", "cadence");
        assertEquals(HttpStatus.BAD_REQUEST, validation.getStatus());
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", validation.getCode());

        ApiErrorException notFoundWithNull = StandingOrderErrors.notFound(null);
        assertEquals(HttpStatus.NOT_FOUND, notFoundWithNull.getStatus());
        assertEquals("standingOrderId", notFoundWithNull.getField());

        ApiErrorException notFoundWithValue = StandingOrderErrors.notFound("so-1");
        assertNull(notFoundWithValue.getField());

        ApiErrorException accountMissing = StandingOrderErrors.accountNotFound("sourceAccountId");
        assertEquals(HttpStatus.NOT_FOUND, accountMissing.getStatus());
        assertEquals("STANDING_ORDER_ACCOUNT_NOT_FOUND", accountMissing.getCode());

        ApiErrorException forbidden = StandingOrderErrors.forbidden("update");
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertEquals("STANDING_ORDER_FORBIDDEN", forbidden.getCode());

        ApiErrorException conflict = StandingOrderErrors.conflict("boom", "field");
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
        assertEquals("STANDING_ORDER_CONFLICT", conflict.getCode());
    }

    @Test
    void statementErrorsExposeExpectedContracts() {
        ApiErrorException validation = StatementErrors.validation("bad", "periodYearMonth");
        assertEquals(HttpStatus.BAD_REQUEST, validation.getStatus());
        assertEquals("STATEMENT_VALIDATION_ERROR", validation.getCode());

        ApiErrorException accountMissing = StatementErrors.accountNotFound("accountId");
        assertEquals(HttpStatus.NOT_FOUND, accountMissing.getStatus());
        assertEquals("STATEMENT_ACCOUNT_NOT_FOUND", accountMissing.getCode());

        ApiErrorException notFoundNull = StatementErrors.notFound(null);
        assertEquals(HttpStatus.NOT_FOUND, notFoundNull.getStatus());
        assertEquals("statementId", notFoundNull.getField());

        ApiErrorException notFoundValue = StatementErrors.notFound("stmt-1");
        assertNull(notFoundValue.getField());

        ApiErrorException forbidden = StatementErrors.forbidden("read");
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
        assertEquals("STATEMENT_FORBIDDEN", forbidden.getCode());

        ApiErrorException conflict = StatementErrors.conflict("dup", "field");
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
        assertEquals("STATEMENT_CONFLICT", conflict.getCode());

        ApiErrorException dependency = StatementErrors.dependencyFailure("offline");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, dependency.getStatus());
        assertEquals("STATEMENT_DEPENDENCY_FAILURE", dependency.getCode());
    }

    @Test
    void notificationTransactionAndInsightErrorsExposeExpectedContracts() {
        ApiErrorException notificationValidation = NotificationErrors.validation("bad", "eventType");
        assertEquals(HttpStatus.BAD_REQUEST, notificationValidation.getStatus());
        assertEquals("NOTIFICATION_VALIDATION_ERROR", notificationValidation.getCode());

        ApiErrorException notificationNotFoundNull = NotificationErrors.notFound(null);
        assertEquals(HttpStatus.NOT_FOUND, notificationNotFoundNull.getStatus());
        assertEquals("notificationEventId", notificationNotFoundNull.getField());

        ApiErrorException notificationNotFoundValue = NotificationErrors.notFound("evt-1");
        assertNull(notificationNotFoundValue.getField());

        ApiErrorException scopeNotFound = NotificationErrors.scopeNotFound("recipientScopeId");
        assertEquals(HttpStatus.NOT_FOUND, scopeNotFound.getStatus());
        assertEquals("NOTIFICATION_SCOPE_NOT_FOUND", scopeNotFound.getCode());

        ApiErrorException notificationForbidden = NotificationErrors.forbidden("read");
        assertEquals(HttpStatus.FORBIDDEN, notificationForbidden.getStatus());
        assertEquals("NOTIFICATION_FORBIDDEN", notificationForbidden.getCode());

        ApiErrorException notificationConflict = NotificationErrors.conflict("dup", "field");
        assertEquals(HttpStatus.CONFLICT, notificationConflict.getStatus());
        assertEquals("NOTIFICATION_CONFLICT", notificationConflict.getCode());

        ApiErrorException txValidation = TransactionErrors.validation("bad", "amount");
        assertEquals(HttpStatus.BAD_REQUEST, txValidation.getStatus());
        assertEquals("TRANSACTION_VALIDATION_ERROR", txValidation.getCode());

        ApiErrorException txAccountMissing = TransactionErrors.accountNotFound("accountId");
        assertEquals(HttpStatus.NOT_FOUND, txAccountMissing.getStatus());

        ApiErrorException txScopeMissing = TransactionErrors.scopeNotFound("scopeId", "missing");
        assertEquals(HttpStatus.NOT_FOUND, txScopeMissing.getStatus());
        assertEquals("TRANSACTION_SCOPE_NOT_FOUND", txScopeMissing.getCode());

        ApiErrorException txForbidden = TransactionErrors.forbidden("read");
        assertEquals(HttpStatus.FORBIDDEN, txForbidden.getStatus());

        ApiErrorException txConflict = TransactionErrors.conflict("dup", "field");
        assertEquals(HttpStatus.CONFLICT, txConflict.getStatus());

        ApiErrorException txIdempotencyConflict = TransactionErrors.idempotencyConflict("dup");
        assertEquals(HttpStatus.CONFLICT, txIdempotencyConflict.getStatus());
        assertEquals("Idempotency-Key", txIdempotencyConflict.getField());

        ApiErrorException insufficientFunds = TransactionErrors.insufficientFunds();
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, insufficientFunds.getStatus());
        assertEquals("TRANSACTION_INSUFFICIENT_FUNDS", insufficientFunds.getCode());

        ApiErrorException insightValidation = InsightErrors.validation("bad", "scopeType");
        assertEquals(HttpStatus.BAD_REQUEST, insightValidation.getStatus());
        assertEquals("INSIGHT_VALIDATION_ERROR", insightValidation.getCode());

        ApiErrorException insightForbidden = InsightErrors.forbidden();
        assertEquals(HttpStatus.FORBIDDEN, insightForbidden.getStatus());
        assertEquals("INSIGHT_FORBIDDEN", insightForbidden.getCode());

        ApiErrorException insightScopeMissing = InsightErrors.scopeNotFound("scopeId");
        assertEquals(HttpStatus.NOT_FOUND, insightScopeMissing.getStatus());
        assertEquals("INSIGHT_SCOPE_NOT_FOUND", insightScopeMissing.getCode());

        ApiErrorException insightDependency = InsightErrors.dependencyFailure("offline");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, insightDependency.getStatus());
        assertEquals("INSIGHT_DEPENDENCY_FAILURE", insightDependency.getCode());
    }
}
