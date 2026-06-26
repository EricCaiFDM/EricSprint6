package com.example.banking.api.customer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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
        "spring.datasource.url=jdbc:h2:mem:customer-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String createPayload(String externalCustomerKey, String legalName, String primaryEmail, String phoneNumber) {
        return "{" +
                "\"externalCustomerKey\":\"" + externalCustomerKey + "\"," +
                "\"legalName\":\"" + legalName + "\"," +
                "\"primaryEmail\":\"" + primaryEmail + "\"," +
                "\"phoneNumber\":\"" + phoneNumber + "\"" +
                "}";
    }

    private MvcResult createCustomer(String actorUserId, String role, String externalKey, String email) throws Exception {
        return mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", actorUserId).claim("role", role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload(externalKey, "Jane Customer", email, "+27123456789")))
                .andReturn();
    }

    @Test
    void createCustomerReturnsCreatedPayload() throws Exception {
        mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "user-100").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("ext-100", "Jane Customer", "jane100@example.com", "+27123456789")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isString())
                .andExpect(jsonPath("$.externalCustomerKey").value("ext-100"))
                .andExpect(jsonPath("$.primaryEmail").value("jane100@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCustomerReturnsConflictForDuplicateBusinessKey() throws Exception {
        mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "user-101").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("ext-duplicate", "Jane Customer", "dup@example.com", "+27123456789")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "user-101").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("ext-duplicate", "Other Name", "dup2@example.com", "+27123456789")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_CONFLICT"))
                .andExpect(jsonPath("$.field").value("externalCustomerKey"));
    }

    @Test
    void getCustomerAppliesMaskingForCustomerRole() throws Exception {
        MvcResult createResult = createCustomer("owner-200", "CUSTOMER", "ext-200", "jane200@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(get("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-200").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmail").value("j***@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("***-***-6789"));
    }

    @Test
    void getCustomerDeniedForOutOfScopeCustomerRole() throws Exception {
        MvcResult createResult = createCustomer("owner-201", "CUSTOMER", "ext-201", "jane201@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(get("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "other-user").claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CUSTOMER_FORBIDDEN"));
    }

    @Test
    void adminCanReadCrossScopeWithoutMasking() throws Exception {
        MvcResult createResult = createCustomer("owner-202", "CUSTOMER", "ext-202", "jane202@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(get("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmail").value("jane202@example.com"));
    }

    @Test
    void patchCustomerUpdatesMutableFields() throws Exception {
        MvcResult createResult = createCustomer("owner-203", "CUSTOMER", "ext-203", "jane203@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        String updatePayload = "{" +
                "\"legalName\":\"Jane Updated\"," +
                "\"status\":\"SUSPENDED\"" +
                "}";

        mockMvc.perform(patch("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-203").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalName").value("Jane Updated"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void patchCustomerRejectsInvalidStatusTransition() throws Exception {
        MvcResult createResult = createCustomer("owner-204", "CUSTOMER", "ext-204", "jane204@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(patch("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-204").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-204").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_VALIDATION_ERROR"));
    }

    @Test
    void deleteCustomerReturnsConflictWhenDependencyExists() throws Exception {
        MvcResult createResult = createCustomer("owner-205", "CUSTOMER", "ext-205", "jane205@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        jdbcTemplate.update(
                "INSERT INTO accounts(account_id, customer_id, balance, currency, created_at) VALUES (?, ?, ?, ?, ?)",
                "acct-1",
                customerId,
                100.00,
                "USD",
                Instant.now());

        mockMvc.perform(delete("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-205").claim("role", "CUSTOMER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_DELETE_BLOCKED"));
    }

    @Test
    void deleteCustomerSuccessRemovesFromOperationalAccess() throws Exception {
        MvcResult createResult = createCustomer("owner-206", "CUSTOMER", "ext-206", "jane206@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(delete("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-206").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        mockMvc.perform(get("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-206").claim("role", "CUSTOMER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }
}
