package com.example.banking.lib.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();
    private final OpenApiCustomizer customizer = config.operationDescriptionCustomizer();

    @Test
    void bankingOpenApiProvidesApiMetadata() {
        OpenAPI openApi = config.bankingOpenApi();

        assertEquals("NorthBridge Banking API", openApi.getInfo().getTitle());
        assertEquals("v1", openApi.getInfo().getVersion());
        assertEquals(
                "Interactive API documentation for the NorthBridge banking backend. "
                        + "Each operation includes a summary and a clear description of business purpose.",
                openApi.getInfo().getDescription());
    }

    @Test
    void customizerReturnsWhenPathsAreMissing() {
        OpenAPI openApi = new OpenAPI();

        assertDoesNotThrow(() -> customizer.customise(openApi));
    }

    @Test
    void customizerSkipsNullPathItemsNullOperationMapsAndNullOperations() {
        OpenAPI openApi = new OpenAPI().paths(new Paths());
        openApi.getPaths().put("/null-item", null);
        openApi.getPaths().put("/null-map", new FixedOperationsPathItem(null));

        Map<PathItem.HttpMethod, Operation> nullOperationMap = new LinkedHashMap<>();
        nullOperationMap.put(PathItem.HttpMethod.GET, null);
        openApi.getPaths().put("/null-operation", new FixedOperationsPathItem(nullOperationMap));

        assertDoesNotThrow(() -> customizer.customise(openApi));
    }

    @Test
    void customizerAppliesKnownOperationSummaryDescriptionAndTag() {
        Operation operation = new Operation();
        OpenAPI openApi = openApiWithOperation("/auth/login", PathItem.HttpMethod.POST, operation);

        customizer.customise(openApi);

        assertEquals("Authenticate user", operation.getSummary());
        assertEquals("Validates credentials and issues an access token and refresh token for authenticated API calls.",
                operation.getDescription());
        assertEquals(List.of("Authentication"), operation.getTags());
    }

    @Test
    void customizerAppliesFallbackSummaryDescriptionAndGeneralTagForUnknownRoute() {
        Operation operation = new Operation();
        OpenAPI openApi = openApiWithOperation("/unknown/endpoint", PathItem.HttpMethod.GET, operation);

        customizer.customise(openApi);

        assertEquals("GET /unknown/endpoint", operation.getSummary());
        assertEquals("Executes the GET operation for /unknown/endpoint.", operation.getDescription());
        assertEquals(List.of("General"), operation.getTags());
    }

    @Test
    void customizerAppliesFallbackWhenSummaryAndDescriptionAreBlank() {
        Operation operation = new Operation();
        operation.setSummary("   ");
        operation.setDescription("\t");
        OpenAPI openApi = openApiWithOperation("/unknown/blank", PathItem.HttpMethod.GET, operation);

        customizer.customise(openApi);

        assertEquals("GET /unknown/blank", operation.getSummary());
        assertEquals("Executes the GET operation for /unknown/blank.", operation.getDescription());
    }

    @Test
    void customizerPreservesExistingSummaryDescriptionAndTags() {
        Operation operation = new Operation();
        operation.setSummary("Keep summary");
        operation.setDescription("Keep description");
        operation.setTags(List.of("ExistingTag"));
        OpenAPI openApi = openApiWithOperation("/unknown/preserve", PathItem.HttpMethod.GET, operation);

        customizer.customise(openApi);

        assertEquals("Keep summary", operation.getSummary());
        assertEquals("Keep description", operation.getDescription());
        assertEquals(List.of("ExistingTag"), operation.getTags());
    }

    @Test
    void customizerSetsTagWhenTagListExistsButIsEmpty() {
        Operation operation = new Operation();
        operation.setTags(List.of());
        OpenAPI openApi = openApiWithOperation("/accounts/custom", PathItem.HttpMethod.GET, operation);

        customizer.customise(openApi);

        assertEquals(List.of("Accounts"), operation.getTags());
    }

    @Test
    void customizerResolvesExpectedTagsForEachRouteFamily() {
        Map<String, String> expectedTags = new LinkedHashMap<>();
        expectedTags.put("/auth/custom", "Authentication");
        expectedTags.put("/accounts/custom", "Accounts");
        expectedTags.put("/customers/custom", "Customers");
        expectedTags.put("/transactions/custom", "Transactions");
        expectedTags.put("/standing-orders/custom", "Standing Orders");
        expectedTags.put("/notifications/custom", "Notifications");
        expectedTags.put("/statements/custom", "Statements");
        expectedTags.put("/insights/custom", "Insights");
        expectedTags.put("/health", "System");
        expectedTags.put("/general/custom", "General");

        OpenAPI openApi = new OpenAPI().paths(new Paths());
        Map<String, Operation> operationsByPath = new LinkedHashMap<>();

        expectedTags.forEach((path, expectedTag) -> {
            Operation operation = new Operation();
            operationsByPath.put(path, operation);
            openApi.getPaths().put(path,
                    new FixedOperationsPathItem(Map.of(PathItem.HttpMethod.GET, operation)));
        });

        customizer.customise(openApi);

        expectedTags.forEach((path, expectedTag) ->
                assertEquals(List.of(expectedTag), operationsByPath.get(path).getTags()));
    }

    private static OpenAPI openApiWithOperation(String path, PathItem.HttpMethod method, Operation operation) {
        OpenAPI openApi = new OpenAPI().paths(new Paths());
        openApi.getPaths().put(path, new FixedOperationsPathItem(Map.of(method, operation)));
        return openApi;
    }

    private static final class FixedOperationsPathItem extends PathItem {
        private final Map<HttpMethod, Operation> operationsMap;

        private FixedOperationsPathItem(Map<HttpMethod, Operation> operationsMap) {
            this.operationsMap = operationsMap;
        }

        @Override
        public Map<HttpMethod, Operation> readOperationsMap() {
            return operationsMap;
        }
    }
}
