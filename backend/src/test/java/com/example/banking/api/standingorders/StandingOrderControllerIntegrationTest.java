package com.example.banking.api.standingorders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.example.banking.services.StandingOrderExecutionOrchestrator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:standing-order-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class StandingOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StandingOrderExecutionOrchestrator executionOrchestrator;

        @Autowired
        private JdbcTemplate jdbcTemplate;

    private String createCustomer(String ownerUserId, String suffix) throws Exception {
        String payload = "{" +
                "\"externalCustomerKey\":\"so-ext-" + suffix + "\"," +
                "\"legalName\":\"Shawn Order\"," +
                "\"primaryEmail\":\"so." + suffix + "@example.com\"," +
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
                                "\"currencyCode\":\"AUD\"," +
                "\"nickname\":\"SO " + accountType + "\"" +
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

    @Test
    void createPauseResumeCancelStandingOrderLifecycle() throws Exception {
        String customerId = createCustomer("so-owner-100", "100");
        String sourceAccountId = createAccount("so-owner-100", customerId, "CHECKING");
        String destinationAccountId = createAccount("so-owner-100", customerId, "SAVINGS");

        String createPayload = "{" +
                "\"sourceAccountId\":\"" + sourceAccountId + "\"," +
                "\"destinationAccountId\":\"" + destinationAccountId + "\"," +
                "\"amount\":\"25.00\"," +
                "\"cadence\":\"MONTHLY\"," +
                "\"effectiveFromUtc\":\"" + Instant.now().plusSeconds(30).toString() + "\"," +
                "\"retryPolicyCode\":\"STANDARD\"" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/standing-orders")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-100").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lifecycleState").value("ACTIVE"))
                .andReturn();

        String standingOrderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("standingOrderId")
                .asText();

        mockMvc.perform(post("/standing-orders/{standingOrderId}/pause", standingOrderId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleState").value("PAUSED"));

        mockMvc.perform(post("/standing-orders/{standingOrderId}/resume", standingOrderId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleState").value("ACTIVE"));

        mockMvc.perform(post("/standing-orders/{standingOrderId}/cancel", standingOrderId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleState").value("CANCELLED"));
    }

    @Test
    void executionOrchestratorPersistsExecutionOutcomeAndRouteListsIt() throws Exception {
        String customerId = createCustomer("so-owner-101", "101");
        String sourceAccountId = createAccount("so-owner-101", customerId, "CHECKING");
        String destinationAccountId = createAccount("so-owner-101", customerId, "SAVINGS");

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-101").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-so-seed-101")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + sourceAccountId + "\",\"amount\":\"150.00\"}"))
                .andExpect(status().isCreated());

        String createPayload = "{" +
                "\"sourceAccountId\":\"" + sourceAccountId + "\"," +
                "\"destinationAccountId\":\"" + destinationAccountId + "\"," +
                "\"amount\":\"40.00\"," +
                "\"cadence\":\"DAILY\"," +
                "\"effectiveFromUtc\":\"" + Instant.now().plusSeconds(20).toString() + "\"," +
                "\"retryPolicyCode\":\"STANDARD\"" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/standing-orders")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-101").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String standingOrderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("standingOrderId")
                .asText();

        executionOrchestrator.processWindow(Instant.now().minusSeconds(60), Instant.now().plusSeconds(120));

        mockMvc.perform(get("/standing-orders/{standingOrderId}/executions", standingOrderId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-101").claim("role", "CUSTOMER")))
                .param("page", "1")
                .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("SUCCEEDED"));

        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_events WHERE event_type = ? AND recipient_scope_type = ? AND recipient_scope_id = ?",
                Integer.class,
                "STANDING_ORDER_EXECUTED",
                "ACCOUNT",
                sourceAccountId);
        org.junit.jupiter.api.Assertions.assertEquals(1, notificationCount);
    }

    @Test
    void updateStandingOrderAllowsAmountAndCadenceChanges() throws Exception {
        String customerId = createCustomer("so-owner-102", "102");
        String sourceAccountId = createAccount("so-owner-102", customerId, "CHECKING");
        String destinationAccountId = createAccount("so-owner-102", customerId, "SAVINGS");

        String createPayload = "{" +
                "\"sourceAccountId\":\"" + sourceAccountId + "\"," +
                "\"destinationAccountId\":\"" + destinationAccountId + "\"," +
                "\"amount\":\"30.00\"," +
                "\"cadence\":\"MONTHLY\"," +
                "\"effectiveFromUtc\":\"" + Instant.now().plusSeconds(30).toString() + "\"" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/standing-orders")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-102").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String standingOrderId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("standingOrderId")
                .asText();

        String updatePayload = "{" +
                "\"amount\":\"45.50\"," +
                "\"cadence\":\"WEEKLY\"" +
                "}";

        mockMvc.perform(patch("/standing-orders/{standingOrderId}", standingOrderId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "so-owner-102").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("45.50"))
                .andExpect(jsonPath("$.cadence").value("WEEKLY"));
    }
}
