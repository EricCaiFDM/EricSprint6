package com.example.banking.api.transactions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
        "spring.datasource.url=jdbc:h2:mem:transaction-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createCustomer(String ownerUserId, String suffix) throws Exception {
        String payload = "{" +
                "\"externalCustomerKey\":\"tx-ext-" + suffix + "\"," +
                "\"legalName\":\"Taylor Transaction\"," +
                "\"primaryEmail\":\"tx." + suffix + "@example.com\"," +
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
                "\"nickname\":\"TX " + accountType + "\"" +
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
    void depositPostsTransactionAndUpdatesBalance() throws Exception {
        String customerId = createCustomer("tx-owner-100", "100");
        String accountId = createAccount("tx-owner-100", customerId, "CHECKING");

        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":\"125.50\"" +
                "}";

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-100").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-deposit-100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.postedAmount").value("125.50"))
                .andExpect(jsonPath("$.balanceAfter").value("125.50"));

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("125.50"))
                .andExpect(jsonPath("$.availableBalance").value("125.50"))
                .andExpect(jsonPath("$.currentBalance").value("125.50"));

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE account_id = ?",
                BigDecimal.class,
                accountId);
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("125.50"), balance);

        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_events WHERE event_type = ? AND recipient_scope_type = ? AND recipient_scope_id = ?",
                Integer.class,
                "DEPOSIT_POSTED",
                "ACCOUNT",
                accountId);
        org.junit.jupiter.api.Assertions.assertEquals(1, notificationCount);
    }

    @Test
    void depositReplayWithSameIdempotencyKeyReturnsDeterministicResponse() throws Exception {
        String customerId = createCustomer("tx-owner-101", "101");
        String accountId = createAccount("tx-owner-101", customerId, "CHECKING");

        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":\"10.00\"" +
                "}";

        MvcResult first = mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-101").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-replay-101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult replay = mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-101").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-replay-101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsString());
        JsonNode replayBody = objectMapper.readTree(replay.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(
                firstBody.get("transactionId").asText(),
                replayBody.get("transactionId").asText());

        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE idempotency_key = ? AND transaction_type = ?",
                Integer.class,
                "idem-replay-101",
                "DEPOSIT");
        org.junit.jupiter.api.Assertions.assertEquals(1, txCount);
    }

    @Test
    void withdrawalRejectsInsufficientFunds() throws Exception {
        String customerId = createCustomer("tx-owner-102", "102");
        String accountId = createAccount("tx-owner-102", customerId, "CHECKING");

        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":\"50.00\"" +
                "}";

        mockMvc.perform(post("/transactions/withdrawal")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-102").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-withdraw-102")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TRANSACTION_INSUFFICIENT_FUNDS"));
    }

    @Test
    void transferPostsAtomicDebitCreditAndPersistsLink() throws Exception {
        String customerId = createCustomer("tx-owner-103", "103");
        String sourceAccountId = createAccount("tx-owner-103", customerId, "CHECKING");
        String destinationAccountId = createAccount("tx-owner-103", customerId, "SAVINGS");

        String seedPayload = "{" +
                "\"accountId\":\"" + sourceAccountId + "\"," +
                "\"amount\":\"200.00\"" +
                "}";

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-103").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-seed-103")
                .contentType(MediaType.APPLICATION_JSON)
                .content(seedPayload))
                .andExpect(status().isCreated());

        String transferPayload = "{" +
                "\"sourceAccountId\":\"" + sourceAccountId + "\"," +
                "\"destinationAccountId\":\"" + destinationAccountId + "\"," +
                "\"amount\":\"75.00\"" +
                "}";

        mockMvc.perform(post("/transactions/transfer")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-103").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-transfer-103")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postedAmount").value("75.00"))
                .andExpect(jsonPath("$.sourceBalanceAfter").value("125.00"))
                .andExpect(jsonPath("$.destinationBalanceAfter").value("75.00"));

        BigDecimal sourceBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE account_id = ?",
                BigDecimal.class,
                sourceAccountId);
        BigDecimal destinationBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE account_id = ?",
                BigDecimal.class,
                destinationAccountId);

        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("125.00"), sourceBalance);
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("75.00"), destinationBalance);

        Integer transferLinks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transfer_links", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, transferLinks);

        Integer transferNotificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_events WHERE event_type = ? AND recipient_scope_type = ? AND recipient_scope_id = ?",
                Integer.class,
                "TRANSFER_COMPLETED",
                "ACCOUNT",
                sourceAccountId);
        org.junit.jupiter.api.Assertions.assertEquals(1, transferNotificationCount);
    }

    @Test
    void historySupportsFilteringPaginationAndScopeAuthorization() throws Exception {
        String customerId = createCustomer("tx-owner-104", "104");
        String accountId = createAccount("tx-owner-104", customerId, "CHECKING");

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-104").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-hist-deposit1-104")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"amount\":\"20.00\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions/withdrawal")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-104").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-hist-withdraw-104")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"amount\":\"4.00\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-104").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-hist-deposit2-104")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountId\":\"" + accountId + "\",\"amount\":\"3.00\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/transactions/history")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-104").claim("role", "CUSTOMER")))
                .queryParam("scopeType", "ACCOUNT")
                .queryParam("scopeId", accountId)
                .queryParam("page", "1")
                .queryParam("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(2));

        mockMvc.perform(get("/transactions/history")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-104").claim("role", "CUSTOMER")))
                .queryParam("scopeType", "ACCOUNT")
                .queryParam("scopeId", accountId)
                .queryParam("transactionType", "WITHDRAWAL")
                .queryParam("page", "1")
                .queryParam("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].transactionType").value("WITHDRAWAL"));

        mockMvc.perform(get("/transactions/history")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-user-104").claim("role", "CUSTOMER")))
                .queryParam("scopeType", "ACCOUNT")
                .queryParam("scopeId", accountId)
                .queryParam("page", "1")
                .queryParam("pageSize", "20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRANSACTION_FORBIDDEN"));
    }

    @Test
    void depositNotificationAppearsInRecentNotificationsFeed() throws Exception {
        String customerId = createCustomer("tx-owner-105", "105");
        String accountId = createAccount("tx-owner-105", customerId, "CHECKING");

        String payload = "{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"amount\":\"50.00\"" +
                "}";

        mockMvc.perform(post("/transactions/deposit")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-105").claim("role", "CUSTOMER")))
                .header("Idempotency-Key", "idem-notif-feed-105")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/notifications/events")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "tx-owner-105").claim("role", "CUSTOMER")))
                .queryParam("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Deposit Posted"))
                .andExpect(jsonPath("$[0].level").value("Info"));
    }
}
