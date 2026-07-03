package com.example.banking.api.account;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        "spring.datasource.url=jdbc:h2:mem:account-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createCustomer(String ownerUserId, String role, String suffix) throws Exception {
        String payload = "{" +
                "\"externalCustomerKey\":\"ext-account-" + suffix + "\"," +
                "\"legalName\":\"Casey Account\"," +
                "\"primaryEmail\":\"casey.account." + suffix + "@example.com\"," +
                "\"phoneNumber\":\"+27123456789\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", ownerUserId).claim("role", role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("customerId").asText();
    }

    private String createAccount(String actorUserId, String role, String customerId, String accountType) throws Exception {
        String payload = "{" +
                "\"customerId\":\"" + customerId + "\"," +
                "\"accountType\":\"" + accountType + "\"," +
                "\"currencyCode\":\"AUD\"," +
                "\"nickname\":\"Main " + accountType + "\"" +
                "}";

        MvcResult result = mockMvc.perform(post("/accounts")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accountId").asText();
    }

    @Test
    void createAndGetAccountWorksForOwner() throws Exception {
        String customerId = createCustomer("owner-400", "CUSTOMER", "400");
        String accountId = createAccount("owner-400", "CUSTOMER", customerId, "CHECKING");

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-400").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.checkingNumber").value(1))
                .andExpect(jsonPath("$.interestRate").value("0.0000"))
                .andExpect(jsonPath("$.balance").value("0.00"))
                .andExpect(jsonPath("$.availableBalance").value("0.00"))
                .andExpect(jsonPath("$.currentBalance").value("0.00"));
    }

    @Test
    void savingsAccountCanSetInterestRate() throws Exception {
        String customerId = createCustomer("owner-400b", "CUSTOMER", "400b");
        String payload = "{" +
                "\"customerId\":\"" + customerId + "\"," +
                "\"accountType\":\"SAVINGS\"," +
                "\"currencyCode\":\"AUD\"," +
                "\"interestRate\":2.7500" +
                "}";

        mockMvc.perform(post("/accounts")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-400b").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.interestRate").value("2.7500"))
                .andExpect(jsonPath("$.checkingNumber").isEmpty());
    }

    @Test
    void checkingNumberIncrementsPerCustomer() throws Exception {
        String customerId = createCustomer("owner-400c", "CUSTOMER", "400c");
        String firstAccountId = createAccount("owner-400c", "CUSTOMER", customerId, "CHECKING");
        String secondAccountId = createAccount("owner-400c", "CUSTOMER", customerId, "CHECKING");

        mockMvc.perform(get("/accounts/{accountId}", firstAccountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-400c").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkingNumber").value(1));

        mockMvc.perform(get("/accounts/{accountId}", secondAccountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-400c").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkingNumber").value(2));
    }

    @Test
    void listAccountsSupportsPaginationAndFilters() throws Exception {
        String customerId = createCustomer("owner-401", "CUSTOMER", "401");
        createAccount("owner-401", "CUSTOMER", customerId, "CHECKING");
        createAccount("owner-401", "CUSTOMER", customerId, "SAVINGS");

        mockMvc.perform(get("/accounts")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-401").claim("role", "CUSTOMER")))
                .queryParam("customerId", customerId)
                .queryParam("page", "1")
                .queryParam("pageSize", "1")
                .queryParam("accountType", "CHECKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].balance").value("0.00"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(1));
    }

    @Test
    void updateAccountAllowsNicknameAndStatus() throws Exception {
        String customerId = createCustomer("owner-402", "CUSTOMER", "402");
        String accountId = createAccount("owner-402", "CUSTOMER", customerId, "CHECKING");

        mockMvc.perform(patch("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-402").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"Bills\",\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Bills"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

        @Test
        void adminCanUpdateSavingsInterestRateAndBalance() throws Exception {
                String customerId = createCustomer("owner-402b", "CUSTOMER", "402b");
                String accountId = createAccount("owner-402b", "CUSTOMER", customerId, "SAVINGS");

                mockMvc.perform(patch("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-402b").claim("role", "ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"interestRate\":3.1000,\"balance\":1250.50}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.interestRate").value("3.1000"))
                                .andExpect(jsonPath("$.balance").value("1250.50"))
                                .andExpect(jsonPath("$.availableBalance").value("1250.50"))
                                .andExpect(jsonPath("$.currentBalance").value("1250.50"));
        }

        @Test
        void customerCannotUpdateSavingsInterestRateOrBalance() throws Exception {
                String customerId = createCustomer("owner-402c", "CUSTOMER", "402c");
                String accountId = createAccount("owner-402c", "CUSTOMER", customerId, "SAVINGS");

                mockMvc.perform(patch("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-402c").claim("role", "CUSTOMER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"interestRate\":2.2500,\"balance\":99.99}"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("ACCOUNT_FORBIDDEN"));
        }

        @Test
        void adminCannotUpdateInterestRateForCheckingAccounts() throws Exception {
                String customerId = createCustomer("owner-402d", "CUSTOMER", "402d");
                String accountId = createAccount("owner-402d", "CUSTOMER", customerId, "CHECKING");

                mockMvc.perform(patch("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-402d").claim("role", "ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"interestRate\":1.0000}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("ACCOUNT_VALIDATION_ERROR"))
                                .andExpect(jsonPath("$.field").value("interestRate"));
        }

    @Test
    void outOfScopeCustomerCannotReadAccount() throws Exception {
        String customerId = createCustomer("owner-403", "CUSTOMER", "403");
        String accountId = createAccount("owner-403", "CUSTOMER", customerId, "CHECKING");

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-user").claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FORBIDDEN"));
    }

    @Test
    void outOfScopeCustomerCannotCreateAccount() throws Exception {
        String ownerCustomerId = createCustomer("owner-403b", "CUSTOMER", "403b");
        createCustomer("other-user-403b", "CUSTOMER", "403c");

        String payload = "{" +
                "\"customerId\":\"" + ownerCustomerId + "\"," +
                "\"accountType\":\"CHECKING\"," +
                "\"currencyCode\":\"AUD\"" +
                "}";

        mockMvc.perform(post("/accounts")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-user-403b").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FORBIDDEN"));
    }

    @Test
    void deleteAccountBlockedWhenBalanceNonZero() throws Exception {
        String customerId = createCustomer("owner-404", "CUSTOMER", "404");
        String accountId = createAccount("owner-404", "CUSTOMER", customerId, "CHECKING");

        jdbcTemplate.update("UPDATE accounts SET balance = ? WHERE account_id = ?", 10.00, accountId);

        mockMvc.perform(delete("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-404").claim("role", "CUSTOMER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DELETE_BLOCKED"));
    }

    @Test
    void deleteAccountSucceedsWhenEligible() throws Exception {
        String customerId = createCustomer("owner-405", "CUSTOMER", "405");
        String accountId = createAccount("owner-405", "CUSTOMER", customerId, "SAVINGS");

        mockMvc.perform(delete("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-405").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-405").claim("role", "CUSTOMER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

        @Test
        void adminCanDeleteCrossScopeAccount() throws Exception {
                String customerId = createCustomer("owner-405b", "CUSTOMER", "405b");
                String accountId = createAccount("owner-405b", "CUSTOMER", customerId, "CHECKING");

                mockMvc.perform(delete("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CLOSED"));

                mockMvc.perform(get("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
        }

        @Test
        void outOfScopeCustomerCannotDeleteAccount() throws Exception {
                String customerId = createCustomer("owner-405c", "CUSTOMER", "405c");
                String accountId = createAccount("owner-405c", "CUSTOMER", customerId, "SAVINGS");

                mockMvc.perform(delete("/accounts/{accountId}", accountId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-user").claim("role", "CUSTOMER"))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("ACCOUNT_FORBIDDEN"));
        }

    @Test
    void adminCanRetrieveCrossScope() throws Exception {
        String customerId = createCustomer("owner-406", "CUSTOMER", "406");
        String accountId = createAccount("owner-406", "CUSTOMER", customerId, "CHECKING");

        mockMvc.perform(get("/accounts/{accountId}", accountId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId));
    }

    @Test
    void malformedAccountIdReturnsValidationError() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", "invalid-id")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT_VALIDATION_ERROR"));
    }

    @Test
    void unsupportedAccountTypeReturnsConflict() throws Exception {
        String customerId = createCustomer("owner-407", "CUSTOMER", "407");

        String payload = "{" +
                "\"customerId\":\"" + customerId + "\"," +
                "\"accountType\":\"BROKERAGE\"," +
                "\"currencyCode\":\"AUD\"" +
                "}";

        mockMvc.perform(post("/accounts")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-407").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CONFLICT"));
    }

        @Test
        void listAccountsAllowsLegacyCreatorScopeWhenOwnerDrifts() throws Exception {
                String customerId = createCustomer("owner-408", "CUSTOMER", "408");
                createAccount("owner-408", "CUSTOMER", customerId, "CHECKING");

                jdbcTemplate.update(
                                "UPDATE customers SET owner_user_id = ? WHERE customer_id = ?",
                                "owner-drift-408",
                                customerId);

                mockMvc.perform(get("/accounts")
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-408").claim("role", "CUSTOMER")))
                                .queryParam("customerId", customerId)
                                .queryParam("page", "1")
                                .queryParam("pageSize", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items.length()").value(1));
        }
}
