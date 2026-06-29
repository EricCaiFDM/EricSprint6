package com.example.banking.api.statements;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import java.time.YearMonth;
import java.time.ZoneOffset;

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
        "spring.datasource.url=jdbc:h2:mem:statement-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class StatementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createCustomer(String ownerUserId, String suffix) throws Exception {
        String payload = "{" +
                "\"externalCustomerKey\":\"stmt-ext-" + suffix + "\"," +
                "\"legalName\":\"Stacy Statement\"," +
                "\"primaryEmail\":\"stmt." + suffix + "@example.com\"," +
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
                "\"nickname\":\"Statements " + accountType + "\"" +
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

    private String generateStatement(String actorUserId, String role, String accountId, String periodYearMonth, String mode)
            throws Exception {
        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"periodYearMonth\":\"" + periodYearMonth + "\"," +
                "\"generationMode\":\"" + mode + "\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/statements/generate")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.generationStatus").value("PROCESSING"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("statementId").asText();
    }

    @Test
    void ownerCanGenerateRetrieveAndListStatements() throws Exception {
        String periodYearMonth = YearMonth.now(ZoneOffset.UTC).toString();
        String customerId = createCustomer("stmt-owner-100", "100");
        String accountId = createAccount("stmt-owner-100", customerId, "CHECKING");

        postDeposit("stmt-owner-100", accountId, "125.50", "idem-stmt-deposit-100");

        String statementId = generateStatement("stmt-owner-100", "CUSTOMER", accountId, periodYearMonth, "STANDARD");

        mockMvc.perform(get("/statements/{statementId}", statementId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "stmt-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value(statementId))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.periodYearMonth").value(periodYearMonth))
                .andExpect(jsonPath("$.artifactVersion").value(1))
                .andExpect(jsonPath("$.status").value("GENERATED"));

        mockMvc.perform(get("/statements/{statementId}/artifact/v{artifactVersion}.pdf", statementId, 1)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "stmt-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("Content-Disposition", containsString("statement-" + periodYearMonth + "-v1.pdf")))
                .andExpect(content().string(startsWith("%PDF")));

        mockMvc.perform(get("/statements")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "stmt-owner-100").claim("role", "CUSTOMER")))
                .queryParam("accountId", accountId)
                .queryParam("periodYearMonth", periodYearMonth)
                .queryParam("page", "1")
                .queryParam("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].statementId").value(statementId))
                .andExpect(jsonPath("$.totalItems").value(1));

        Integer allowedRetrievalEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM statement_retrieval_events WHERE statement_id = ? AND outcome = ?",
                Integer.class,
                statementId,
                "ALLOWED");
        org.junit.jupiter.api.Assertions.assertEquals(2, allowedRetrievalEvents);
    }

    @Test
    void correctionGenerationProducesVersionTwoArtifact() throws Exception {
        String periodYearMonth = YearMonth.now(ZoneOffset.UTC).toString();
        String customerId = createCustomer("stmt-owner-101", "101");
        String accountId = createAccount("stmt-owner-101", customerId, "CHECKING");

        postDeposit("stmt-owner-101", accountId, "50.00", "idem-stmt-deposit-101");

        String versionOneStatementId = generateStatement("stmt-owner-101", "CUSTOMER", accountId, periodYearMonth, "STANDARD");
        String versionTwoStatementId = generateStatement("stmt-owner-101", "CUSTOMER", accountId, periodYearMonth, "CORRECTION");

        mockMvc.perform(get("/statements/{statementId}", versionTwoStatementId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "stmt-owner-101").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifactVersion").value(2))
                .andExpect(jsonPath("$.status").value("CORRECTED"));

        mockMvc.perform(get("/statements")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "stmt-owner-101").claim("role", "CUSTOMER")))
                .queryParam("accountId", accountId)
                .queryParam("periodYearMonth", periodYearMonth)
                .queryParam("page", "1")
                .queryParam("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        org.junit.jupiter.api.Assertions.assertNotEquals(versionOneStatementId, versionTwoStatementId);
    }

    @Test
    void outOfScopeCustomerIsDeniedWhileAdminCanRetrieve() throws Exception {
        String periodYearMonth = YearMonth.now(ZoneOffset.UTC).toString();
        String customerId = createCustomer("stmt-owner-102", "102");
        String accountId = createAccount("stmt-owner-102", customerId, "CHECKING");

        postDeposit("stmt-owner-102", accountId, "20.00", "idem-stmt-deposit-102");
        String statementId = generateStatement("stmt-owner-102", "CUSTOMER", accountId, periodYearMonth, "STANDARD");

        mockMvc.perform(get("/statements/{statementId}", statementId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-customer-102").claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STATEMENT_FORBIDDEN"));

        mockMvc.perform(get("/statements/{statementId}/artifact/v{artifactVersion}.pdf", statementId, 1)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-customer-102").claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STATEMENT_FORBIDDEN"));

        mockMvc.perform(get("/statements/{statementId}", statementId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-102").claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value(statementId));

        mockMvc.perform(get("/statements/{statementId}/artifact/v{artifactVersion}.pdf", statementId, 1)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-102").claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));

        mockMvc.perform(get("/statements")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-customer-102").claim("role", "CUSTOMER")))
                .queryParam("accountId", accountId)
                .queryParam("page", "1")
                .queryParam("pageSize", "20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STATEMENT_FORBIDDEN"));
    }
}
