package com.example.banking.api.auth;

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
        "spring.datasource.url=jdbc:h2:mem:auth-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

        private static String registerPayload(String email, String password, String passwordConfirmation) {
                return registerPayload(email, password, passwordConfirmation, null);
        }

        private static String registerPayload(String email, String password, String passwordConfirmation, String role) {
                String roleSegment = role == null ? "" : ",\"role\":\"" + role + "\"";
                return "{\"email\":\"" + email + "\",\"password\":\"" + password
                                + "\",\"passwordConfirmation\":\"" + passwordConfirmation + "\"" + roleSegment + "}";
    }

    private static String loginPayload(String identity, String password) {
        return "{\"identity\":\"" + identity + "\",\"password\":\"" + password + "\"}";
    }

    private static String passwordResetPayload(String identity) {
        return "{\"identity\":\"" + identity + "\"}";
    }

    private static String refreshPayload(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    @Test
    void registerThenLoginReturnsTokensWithExpiry() throws Exception {
        String registerPayload = registerPayload("jane@example.com", "secret123", "secret123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.userId").isString());

        String loginPayload = loginPayload("jane@example.com", "secret123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void registerReturnsBadRequestWhenPasswordConfirmationMismatches() throws Exception {
        String registerPayload = registerPayload("mismatch@example.com", "secret123", "different123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Password confirmation mismatch"));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        String registerPayload = registerPayload("duplicate@example.com", "secret123", "secret123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_IDENTITY"))
                .andExpect(jsonPath("$.message").value("Email already registered with role CUSTOMER"));
    }

    @Test
    void registerCreatesAdminAccountWhenRoleIsAdmin() throws Exception {
        String payload = registerPayload("admin.new@example.com", "secret123", "secret123", "ADMIN");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        String role = jdbcTemplate.queryForObject(
                "SELECT role FROM auth_users WHERE email = ?",
                String.class,
                "admin.new@example.com");

        org.junit.jupiter.api.Assertions.assertEquals("ADMIN", role);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("admin.new@example.com", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());
    }

    @Test
    void registerRejectsUnsupportedRole() throws Exception {
        String payload = registerPayload("invalid-role@example.com", "secret123", "secret123", "MANAGER");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Unsupported role. Allowed values: CUSTOMER, ADMIN"));
    }

    @Test
    void loginReturnsUnauthorizedForUnknownIdentity() throws Exception {
        String loginPayload = loginPayload("missing@example.com", "secret123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials or ineligible account state"));
    }

    @Test
    void loginReturnsUnauthorizedForWrongPassword() throws Exception {
        String registerPayload = registerPayload("wrongpass@example.com", "secret123", "secret123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated());

        String loginPayload = loginPayload("wrongpass@example.com", "invalid123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials or ineligible account state"));
    }

    @Test
    void loginReturnsUnauthorizedForIneligibleAccountState() throws Exception {
        String registerPayload = registerPayload("inactive@example.com", "secret123", "secret123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated());

        jdbcTemplate.update("UPDATE auth_users SET account_status = ? WHERE email = ?", "SUSPENDED", "inactive@example.com");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("inactive@example.com", "secret123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid credentials or ineligible account state"));
    }

    @Test
    void passwordResetRequestReturnsGenericAcknowledgeForKnownAndUnknownIdentity() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload("known@example.com", "secret123", "secret123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordResetPayload("known@example.com")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message").value("If the account exists, reset instructions will be sent."));

        mockMvc.perform(post("/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordResetPayload("unknown@example.com")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message").value("If the account exists, reset instructions will be sent."));
    }

    @Test
    void tokenRefreshReturnsRotatedTokensForValidRefreshToken() throws Exception {
        String registerPayload = registerPayload("refresh-user@example.com", "secret123", "secret123");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("refresh-user@example.com", "secret123")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        mockMvc.perform(post("/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshPayload(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void tokenRefreshReturnsUnauthorizedForInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/token/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshPayload("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }
}
