package com.example.banking.api.insights;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:insights-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class SpendingInsightControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createCustomer(String ownerUserId, String suffix) throws Exception {
        String payload = "{" +
                "\"externalCustomerKey\":\"ins-ext-" + suffix + "\"," +
                "\"legalName\":\"Ingrid Insight\"," +
                "\"primaryEmail\":\"ins." + suffix + "@example.com\"," +
                "\"phoneNumber\":\"+27123456789\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", ownerUserId).claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("customerId").asText();
    }

    private String createAccount(String actorUserId, String customerId, String accountType) throws Exception {
        String payload = "{" +
                "\"customerId\":\"" + customerId + "\"," +
                "\"accountType\":\"" + accountType + "\"," +
                "\"currencyCode\":\"USD\"," +
                "\"nickname\":\"Insights " + accountType + "\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/accounts")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accountId").asText();
    }

    private void postDeposit(String actorUserId, String accountId, String amount, String idempotencyKey) throws Exception {
        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":\"" + amount + "\"" +
                "}";

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", "CUSTOMER")))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());
    }

    private void postWithdrawal(String actorUserId, String accountId, String amount, String idempotencyKey) throws Exception {
        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":\"" + amount + "\"" +
                "}";

        mockMvc.perform(post("/transactions/withdrawal")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", "CUSTOMER")))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());
    }

    private void postTransfer(
            String actorUserId,
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String idempotencyKey) throws Exception {
        String payload = "{" +
                "\"sourceAccountId\":\"" + sourceAccountId + "\"," +
                "\"destinationAccountId\":\"" + destinationAccountId + "\"," +
                "\"amount\":\"" + amount + "\"" +
                "}";

        mockMvc.perform(post("/transactions/transfer")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", "CUSTOMER")))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCanRetrieveSpendingInsightsWithConfidenceAndMethodology() throws Exception {
        String customerId = createCustomer("ins-owner-100", "100");
        String accountId = createAccount("ins-owner-100", customerId, "CHECKING");
        String savingsAccountId = createAccount("ins-owner-100", customerId, "SAVINGS");

        postDeposit("ins-owner-100", accountId, "200.00", "idem-ins-deposit-100");
        postWithdrawal("ins-owner-100", accountId, "40.00", "idem-ins-withdraw-100");
        postTransfer("ins-owner-100", accountId, savingsAccountId, "50.00", "idem-ins-transfer-100");
        postWithdrawal("ins-owner-100", accountId, "10.00", "idem-ins-withdraw-101");

        mockMvc.perform(get("/insights/spending")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "ins-owner-100").claim("role", "CUSTOMER")))
                .queryParam("scopeType", "ACCOUNT")
                .queryParam("scopeId", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeType").value("ACCOUNT"))
                .andExpect(jsonPath("$.scopeId").value(accountId))
                .andExpect(jsonPath("$.totalSpend").value("100.00"))
                .andExpect(jsonPath("$.confidenceLabel").value("Medium confidence"))
                .andExpect(jsonPath("$.confidenceLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.coverageRatio").value("100.00"))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.methodology", containsString("posted debit transactions")))
                .andExpect(jsonPath("$.categories.length()").value(2));

        Integer allowedEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM insight_retrieval_events WHERE requester_user_id = ? AND outcome = ?",
                Integer.class,
                "ins-owner-100",
                "ALLOWED");
        Assertions.assertNotNull(allowedEvents);
        Assertions.assertTrue(allowedEvents >= 1);
    }

    @Test
    void outOfScopeCustomerIsDeniedFromSpendingInsights() throws Exception {
        String customerId = createCustomer("ins-owner-101", "101");
        String accountId = createAccount("ins-owner-101", customerId, "CHECKING");

        postDeposit("ins-owner-101", accountId, "80.00", "idem-ins-deposit-101");
        postWithdrawal("ins-owner-101", accountId, "10.00", "idem-ins-withdraw-102");

        mockMvc.perform(get("/insights/spending")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-user-101").claim("role", "CUSTOMER")))
                .queryParam("scopeType", "ACCOUNT")
                .queryParam("scopeId", accountId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSIGHT_FORBIDDEN"));
    }

    @Test
    void unsupportedCategoryFilterReturnsValidationError() throws Exception {
        String customerId = createCustomer("ins-owner-102", "102");
        String accountId = createAccount("ins-owner-102", customerId, "CHECKING");

        postDeposit("ins-owner-102", accountId, "25.00", "idem-ins-deposit-102");
        postWithdrawal("ins-owner-102", accountId, "5.00", "idem-ins-withdraw-103");

        mockMvc.perform(get("/insights/spending")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "ins-owner-102").claim("role", "CUSTOMER")))
                .queryParam("scopeType", "ACCOUNT")
                .queryParam("scopeId", accountId)
                .queryParam("categoryFilters", "TRAVEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSIGHT_VALIDATION_ERROR"));
    }
}
