package com.example.banking.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-test-db;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

        private static String registerPayload(String email, String password, String passwordConfirmation) {
                return "{\"email\":\"" + email + "\",\"password\":\"" + password
                                + "\",\"passwordConfirmation\":\"" + passwordConfirmation + "\"}";
        }

        private static String loginPayload(String identity, String password) {
                return "{\"identity\":\"" + identity + "\",\"password\":\"" + password + "\"}";
        }

    @Test
    void registerThenLoginReturnsTokens() throws Exception {
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
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerReturnsBadRequestWhenPasswordConfirmationMismatches() throws Exception {
                                String registerPayload = registerPayload("mismatch@example.com", "secret123", "different123");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isBadRequest())
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
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void loginReturnsUnauthorizedForUnknownIdentity() throws Exception {
                                String loginPayload = loginPayload("missing@example.com", "secret123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
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
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
