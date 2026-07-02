package com.example.banking.api.customer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.hasItem;
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
                return createPayload(externalCustomerKey, legalName, primaryEmail, phoneNumber, null);
        }

        private String createPayload(String externalCustomerKey, String legalName, String primaryEmail, String phoneNumber, String password) {
                String passwordSegment = password == null ? "" : ",\"password\":\"" + password + "\"";
                return "{" +
                                "\"externalCustomerKey\":\"" + externalCustomerKey + "\"," +
                                "\"legalName\":\"" + legalName + "\"," +
                                "\"primaryEmail\":\"" + primaryEmail + "\"," +
                                "\"phoneNumber\":\"" + phoneNumber + "\"" +
                                passwordSegment +
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
    void adminCreateCustomerWithPasswordCreatesLoginReadyCustomerOwner() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-100").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("ext-admin-100", "Admin Provisioned", "admin.provisioned@example.com", "+27123456789", "secret123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryEmail").value("admin.provisioned@example.com"))
                .andExpect(jsonPath("$.createdByUserId").value("admin-100"))
                .andExpect(jsonPath("$.ownerUserId").isString())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String ownerUserId = created.get("ownerUserId").asText();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"admin.provisioned@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        String storedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM auth_users WHERE email = ?",
                String.class,
                "admin.provisioned@example.com");

        org.junit.jupiter.api.Assertions.assertEquals("CUSTOMER", storedRole);
        org.junit.jupiter.api.Assertions.assertNotEquals("admin-100", ownerUserId);
    }

    @Test
    void adminCreateCustomerRequiresPassword() throws Exception {
        mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-101").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload("ext-admin-101", "Missing Password", "admin.missing.password@example.com", "+27123456789")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOMER_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.field").value("password"));
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
    void adminCanListAllCustomers() throws Exception {
        MvcResult firstCreateResult = createCustomer("owner-list-1", "CUSTOMER", "ext-list-1", "list1@example.com");
        String firstCustomerId = objectMapper.readTree(firstCreateResult.getResponse().getContentAsString())
                .get("customerId")
                .asText();

        MvcResult secondCreateResult = createCustomer("owner-list-2", "CUSTOMER", "ext-list-2", "list2@example.com");
        String secondCustomerId = objectMapper.readTree(secondCreateResult.getResponse().getContentAsString())
                .get("customerId")
                .asText();

        mockMvc.perform(get("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-list-user").claim("role", "ADMIN")))
                .queryParam("page", "1")
                .queryParam("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].customerId", hasItem(firstCustomerId)))
                .andExpect(jsonPath("$.items[*].customerId", hasItem(secondCustomerId)));
    }

    @Test
    void customerCannotListAllCustomers() throws Exception {
        mockMvc.perform(get("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "customer-list-user").claim("role", "CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CUSTOMER_FORBIDDEN"));
    }

        @Test
        void getCurrentCustomerResolvesByOwnerUserId() throws Exception {
                MvcResult createResult = createCustomer("owner-202b", "CUSTOMER", "ext-202b", "jane202b@example.com");
                JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
                String customerId = created.get("customerId").asText();

                mockMvc.perform(get("/customers/me")
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-202b").claim("role", "CUSTOMER"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.customerId").value(customerId));
        }

        @Test
        void getCurrentCustomerRecoversLegacyCreatorScopeWhenOwnerDrifts() throws Exception {
                MvcResult createResult = createCustomer("owner-202c", "CUSTOMER", "ext-202c", "jane202c@example.com");
                JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
                String customerId = created.get("customerId").asText();

                jdbcTemplate.update(
                                "UPDATE customers SET owner_user_id = ? WHERE customer_id = ?",
                                "owner-drift-202c",
                                customerId);

                mockMvc.perform(get("/customers/me")
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-202c").claim("role", "CUSTOMER"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.customerId").value(customerId))
                                .andExpect(jsonPath("$.ownerUserId").value("owner-202c"));
        }

        @Test
        void getCurrentCustomerReturnsNotFoundWhenNoProfileLinked() throws Exception {
                mockMvc.perform(get("/customers/me")
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-no-profile").claim("role", "CUSTOMER"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
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
    void patchCustomerPrimaryEmailSyncsLoginIdentity() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-identity-sync").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload(
                        "ext-identity-sync",
                        "Identity Sync",
                        "original.identity@example.com",
                        "+27123456789",
                        "secret123")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(patch("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-identity-sync").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"primaryEmail\":\"renamed.identity@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmail").value("renamed.identity@example.com"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"original.identity@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"renamed.identity@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
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
                "INSERT INTO accounts(account_id, customer_id, account_number, account_type, status, nickname, balance, currency_code, opened_at_utc, closed_at_utc, created_by_user_id, owner_user_id, updated_at_utc, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "00000000-0000-0000-0000-000000000001",
                customerId,
                "NBDEPEND000001",
                "CHECKING",
                "ACTIVE",
                "Dependency Account",
                100.00,
                "USD",
                Instant.now(),
                null,
                "owner-205",
                customerId,
                Instant.now(),
                null);

        mockMvc.perform(delete("/customers/{customerId}", customerId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_DELETE_BLOCKED"));
    }

    @Test
    void customerCanCloseOwnProfileWhenDependencyExists() throws Exception {
        MvcResult createResult = createCustomer("owner-205c", "CUSTOMER", "ext-205c", "jane205c@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        jdbcTemplate.update(
                "INSERT INTO accounts(account_id, customer_id, account_number, account_type, status, nickname, balance, currency_code, opened_at_utc, closed_at_utc, created_by_user_id, owner_user_id, updated_at_utc, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "00000000-0000-0000-0000-000000000011",
                customerId,
                "NBDEPEND000011",
                "CHECKING",
                "ACTIVE",
                "Dependency Account",
                50.00,
                "USD",
                Instant.now(),
                null,
                "owner-205c",
                customerId,
                Instant.now(),
                null);

        mockMvc.perform(delete("/customers/{customerId}", customerId)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-205c").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        mockMvc.perform(get("/customers/{customerId}", customerId)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-205c").claim("role", "CUSTOMER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

        @Test
        void customerCanDeleteOwnCustomerProfile() throws Exception {
                MvcResult createResult = createCustomer("owner-205b", "CUSTOMER", "ext-205b", "jane205b@example.com");
                JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
                String customerId = created.get("customerId").asText();

                mockMvc.perform(delete("/customers/{customerId}", customerId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-205b").claim("role", "CUSTOMER"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DELETED"));

                mockMvc.perform(get("/customers/{customerId}", customerId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-205b").claim("role", "CUSTOMER"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
        }

    @Test
    void customerClosureBlocksFutureLogin() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-close-001").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload(
                        "ext-close-001",
                        "Close Login",
                        "close.login@example.com",
                        "+27123456789",
                        "secret123")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();
        String ownerUserId = created.get("ownerUserId").asText();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"close.login@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        mockMvc.perform(delete("/customers/{customerId}", customerId)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", ownerUserId).claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"close.login@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT account_status FROM auth_users WHERE email = ?",
                String.class,
                "close.login@example.com");
        org.junit.jupiter.api.Assertions.assertEquals("CLOSED", storedStatus);
    }

    @Test
    void customerEmailCanBeReusedAfterClosure() throws Exception {
        MvcResult initialCreateResult = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-reuse-001").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload(
                        "ext-reuse-001",
                        "Reuse Email",
                        "reuse.email@example.com",
                        "+27123456789",
                        "secret123")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode initialCreated = objectMapper.readTree(initialCreateResult.getResponse().getContentAsString());
        String initialCustomerId = initialCreated.get("customerId").asText();
        String initialOwnerUserId = initialCreated.get("ownerUserId").asText();

        mockMvc.perform(delete("/customers/{customerId}", initialCustomerId)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", initialOwnerUserId).claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        MvcResult recreatedResult = mockMvc.perform(post("/customers")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-reuse-001").claim("role", "ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload(
                        "ext-reuse-001",
                        "Reuse Email Again",
                        "reuse.email@example.com",
                        "+27123456789",
                        "secret456")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalCustomerKey").value("ext-reuse-001"))
                .andExpect(jsonPath("$.primaryEmail").value("reuse.email@example.com"))
                .andReturn();

        JsonNode recreated = objectMapper.readTree(recreatedResult.getResponse().getContentAsString());
        String recreatedOwnerUserId = recreated.get("ownerUserId").asText();
        org.junit.jupiter.api.Assertions.assertEquals(initialOwnerUserId, recreatedOwnerUserId);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"reuse.email@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identity\":\"reuse.email@example.com\",\"password\":\"secret456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT account_status FROM auth_users WHERE email = ?",
                String.class,
                "reuse.email@example.com");
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", storedStatus);
    }

    @Test
    void deleteCustomerSuccessRemovesFromOperationalAccess() throws Exception {
        MvcResult createResult = createCustomer("owner-206", "CUSTOMER", "ext-206", "jane206@example.com");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = created.get("customerId").asText();

        mockMvc.perform(delete("/customers/{customerId}", customerId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "admin-user").claim("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        mockMvc.perform(get("/customers/{customerId}", customerId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "owner-206").claim("role", "CUSTOMER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }
}
