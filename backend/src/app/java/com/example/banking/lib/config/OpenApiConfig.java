package com.example.banking.lib.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {
    private static final Map<String, OperationDoc> OPERATION_DOCS = buildOperationDocs();

    @Bean
    OpenAPI bankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NorthBridge Banking API")
                        .version("v1")
                        .description("Interactive API documentation for the NorthBridge banking backend. "
                                + "Each operation includes a summary and a clear description of business purpose."));
    }

    @Bean
    OpenApiCustomizer operationDescriptionCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (pathItem == null || pathItem.readOperationsMap() == null) {
                    return;
                }

                pathItem.readOperationsMap().forEach((method, operation) -> {
                    applyOperationDoc(operation, method.name(), path);
                    applyTagIfMissing(operation, path);
                });
            });
        };
    }

    private static void applyOperationDoc(Operation operation, String method, String path) {
        if (operation == null) {
            return;
        }

        OperationDoc operationDoc = OPERATION_DOCS.get(operationKey(method, path));
        if (operationDoc != null) {
            operation.setSummary(operationDoc.summary());
            operation.setDescription(operationDoc.description());
            return;
        }

        String normalizedMethod = method.toUpperCase(Locale.ROOT);
        if (operation.getSummary() == null || operation.getSummary().isBlank()) {
            operation.setSummary(normalizedMethod + " " + path);
        }
        if (operation.getDescription() == null || operation.getDescription().isBlank()) {
            operation.setDescription("Executes the " + normalizedMethod + " operation for " + path + ".");
        }
    }

    private static void applyTagIfMissing(Operation operation, String path) {
        if (operation == null) {
            return;
        }
        if (operation.getTags() != null && !operation.getTags().isEmpty()) {
            return;
        }
        operation.setTags(List.of(resolveTag(path)));
    }

    private static String resolveTag(String path) {
        if (path.startsWith("/auth")) {
            return "Authentication";
        }
        if (path.startsWith("/accounts")) {
            return "Accounts";
        }
        if (path.startsWith("/customers")) {
            return "Customers";
        }
        if (path.startsWith("/transactions")) {
            return "Transactions";
        }
        if (path.startsWith("/standing-orders")) {
            return "Standing Orders";
        }
        if (path.startsWith("/notifications")) {
            return "Notifications";
        }
        if (path.startsWith("/statements")) {
            return "Statements";
        }
        if (path.startsWith("/insights")) {
            return "Insights";
        }
        if (path.equals("/health")) {
            return "System";
        }
        return "General";
    }

    private static Map<String, OperationDoc> buildOperationDocs() {
        Map<String, OperationDoc> docs = new LinkedHashMap<>();

        putDoc(docs, "GET", "/health", "Health check",
                "Returns a lightweight service status response so clients and probes can confirm the API is online.");

        putDoc(docs, "POST", "/auth/register", "Register a user",
                "Creates a new user account with role-aware validation and returns the created user identifier.");
        putDoc(docs, "POST", "/auth/login", "Authenticate user",
                "Validates credentials and issues an access token and refresh token for authenticated API calls.");
        putDoc(docs, "POST", "/auth/password-reset/request", "Request password reset",
                "Accepts a password reset request and returns a generic acknowledgment to avoid identity disclosure.");
        putDoc(docs, "POST", "/auth/password-reset/confirm", "Confirm password reset",
                "Applies a new password for the requested identity after confirmation and validation checks.");
        putDoc(docs, "POST", "/auth/token/refresh", "Refresh access token",
                "Rotates refresh credentials and returns a fresh access token for continuing authenticated sessions.");

        putDoc(docs, "POST", "/accounts", "Create account",
                "Creates a new bank account within the authorized customer scope and returns account details.");
        putDoc(docs, "GET", "/accounts", "List accounts",
                "Returns a paginated, filterable list of accounts for the selected customer scope.");
        putDoc(docs, "GET", "/accounts/{accountId}", "Get account by id",
                "Returns account details, balances, and metadata for a single authorized account identifier.");
        putDoc(docs, "PATCH", "/accounts/{accountId}", "Update account",
                "Updates mutable account attributes such as nickname or lifecycle state for an authorized account.");
        putDoc(docs, "DELETE", "/accounts/{accountId}", "Delete account",
                "Removes an account from normal operational access according to account policy and authorization rules.");

        putDoc(docs, "POST", "/customers", "Create customer",
                "Creates a new customer profile record and links ownership based on authenticated role and scope.");
        putDoc(docs, "GET", "/customers", "List customers",
                "Returns a paginated customer directory for authorized administrative access.");
        putDoc(docs, "GET", "/customers/{customerId}", "Get customer by id",
                "Returns a single customer profile by identifier when the caller is authorized for that customer.");
        putDoc(docs, "GET", "/customers/me", "Get current customer",
                "Resolves and returns the customer profile associated with the authenticated user identity.");
        putDoc(docs, "PATCH", "/customers/{customerId}", "Update customer",
                "Updates mutable customer profile fields such as legal name, contact information, or status.");
        putDoc(docs, "DELETE", "/customers/{customerId}", "Delete customer",
                "Removes a customer from normal operational access subject to policy and authorization checks.");

        putDoc(docs, "POST", "/transactions/deposit", "Post deposit",
                "Credits funds to an account using idempotency protection and returns the posting transaction details.");
        putDoc(docs, "POST", "/transactions/transfer", "Post transfer",
                "Moves funds between source and destination accounts with idempotency safeguards and audit tracking.");
        putDoc(docs, "POST", "/transactions/withdrawal", "Post withdrawal",
                "Debits funds from an account using idempotency protection and returns the posting transaction details.");
        putDoc(docs, "GET", "/transactions/history", "Get transaction history",
                "Returns paginated transaction history filtered by scope, date range, and other optional query criteria.");

        putDoc(docs, "POST", "/standing-orders", "Create standing order",
                "Creates a recurring payment instruction with cadence and execution policy within authorized scope.");
        putDoc(docs, "GET", "/standing-orders", "List standing orders",
                "Returns paginated standing order instructions visible to the authenticated user scope.");
        putDoc(docs, "GET", "/standing-orders/{standingOrderId}/executions", "List standing order executions",
                "Returns paginated execution events and outcomes for a single standing order.");
        putDoc(docs, "PATCH", "/standing-orders/{standingOrderId}", "Update standing order",
                "Updates editable standing order properties such as amount, schedule, or destination details.");
        putDoc(docs, "POST", "/standing-orders/{standingOrderId}/pause", "Pause standing order",
                "Transitions an active standing order into a paused state to stop future automatic executions.");
        putDoc(docs, "POST", "/standing-orders/{standingOrderId}/resume", "Resume standing order",
                "Reactivates a paused standing order so scheduled executions continue.");
        putDoc(docs, "POST", "/standing-orders/{standingOrderId}/cancel", "Cancel standing order",
                "Cancels a standing order and prevents future executions under that instruction.");

        putDoc(docs, "GET", "/notifications/events", "List recent notifications",
                "Returns a recent notifications feed for the authenticated scope with optional size control.");
        putDoc(docs, "POST", "/notifications/events", "Trigger notification event",
                "Creates a notification event for dispatch processing and returns the accepted event reference.");
        putDoc(docs, "GET", "/notifications/events/{notificationEventId}", "Get notification event",
                "Returns current status and delivery outcome details for a specific notification event.");
        putDoc(docs, "GET", "/notifications/events/{notificationEventId}/attempts", "List notification attempts",
                "Returns paginated dispatch attempts and channel outcomes for a notification event.");
        putDoc(docs, "GET", "/notifications/preferences", "Get notification preferences",
                "Returns current channel and marketing notification preferences for the authenticated user.");
        putDoc(docs, "PATCH", "/notifications/preferences", "Update notification preferences",
                "Updates channel and marketing notification preferences for the authenticated user.");

        putDoc(docs, "POST", "/statements/generate", "Generate statement",
                "Initiates statement generation for an account and month and returns an accepted processing response.");
        putDoc(docs, "GET", "/statements", "List statements",
                "Returns paginated statements for an account, with optional filtering by statement month.");
        putDoc(docs, "GET", "/statements/{statementId}", "Get statement by id",
                "Returns statement metadata, status, and summary details for a single statement identifier.");
        putDoc(docs, "GET", "/statements/{statementId}/artifact/v{artifactVersion}.pdf", "Download statement artifact",
                "Downloads the generated PDF artifact for the requested statement and artifact version.");

        putDoc(docs, "GET", "/insights/spending", "Get spending insights",
                "Returns categorized spending summaries, trends, and confidence metadata for the requested period scope.");

        return Map.copyOf(docs);
    }

    private static void putDoc(
            Map<String, OperationDoc> docs,
            String method,
            String path,
            String summary,
            String description) {
        docs.put(operationKey(method, path), new OperationDoc(summary, description));
    }

    private static String operationKey(String method, String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path;
    }

    private record OperationDoc(String summary, String description) {
    }
}
