package com.example.banking.api.notifications;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createCustomer(String ownerUserId, String suffix) throws Exception {
        String payload = "{" +
                "\"externalCustomerKey\":\"notif-ext-" + suffix + "\"," +
                "\"legalName\":\"Nora Notify\"," +
                "\"primaryEmail\":\"notif." + suffix + "@example.com\"," +
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

    @Test
    void triggerEventRecordsDeliveryAndExposesStatusAndAttempts() throws Exception {
        String customerId = createCustomer("notif-owner-100", "100");

        String triggerPayload = "{" +
                "\"eventType\":\"TRANSACTION_POSTED\"," +
                "\"recipientScopeType\":\"CUSTOMER\"," +
                "\"recipientScopeId\":\"" + customerId + "\"," +
                "\"templateCode\":\"ACCOUNT_ACTIVITY\"," +
                "\"templateContext\":{\"title\":\"Account activity\"}" +
                "}";

        MvcResult triggerResult = mockMvc.perform(post("/notifications/events")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-100").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(triggerPayload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        String notificationEventId = objectMapper.readTree(triggerResult.getResponse().getContentAsString())
                .get("notificationEventId")
                .asText();

        mockMvc.perform(get("/notifications/events/{notificationEventId}", notificationEventId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-100").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalOutcome").value("DELIVERED"));

        mockMvc.perform(get("/notifications/events/{notificationEventId}/attempts", notificationEventId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-100").claim("role", "CUSTOMER")))
                .param("page", "1")
                .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("SUCCEEDED"));
    }

    @Test
    void restrictedPreferencesBlockDeliveryAndRecordBlockedAttempt() throws Exception {
        String customerId = createCustomer("notif-owner-101", "101");

        String triggerPayload = "{" +
                "\"eventType\":\"PROMOTION\"," +
                "\"recipientScopeType\":\"CUSTOMER\"," +
                "\"recipientScopeId\":\"" + customerId + "\"," +
                "\"templateCode\":\"PROMO_ALERT\"," +
                "\"templateContext\":{\"consentStatus\":\"RESTRICTED\"}" +
                "}";

        MvcResult triggerResult = mockMvc.perform(post("/notifications/events")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-101").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(triggerPayload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andReturn();

        String notificationEventId = objectMapper.readTree(triggerResult.getResponse().getContentAsString())
                .get("notificationEventId")
                .asText();

        mockMvc.perform(get("/notifications/events/{notificationEventId}", notificationEventId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-101").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.finalOutcome").value("BLOCKED_RESTRICTED"));

        mockMvc.perform(get("/notifications/events/{notificationEventId}/attempts", notificationEventId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-101").claim("role", "CUSTOMER")))
                .param("page", "1")
                .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void fallbackAttemptIsRecordedWhenPrimaryChannelUnavailable() throws Exception {
        String customerId = createCustomer("notif-owner-102", "102");

        String triggerPayload = "{" +
                "\"eventType\":\"BALANCE_LOW\"," +
                "\"recipientScopeType\":\"CUSTOMER\"," +
                "\"recipientScopeId\":\"" + customerId + "\"," +
                "\"templateCode\":\"BALANCE_WARNING\"," +
                "\"templateContext\":{\"simulateChannelUnavailable\":true}" +
                "}";

        MvcResult triggerResult = mockMvc.perform(post("/notifications/events")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-102").claim("role", "CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(triggerPayload))
                .andExpect(status().isAccepted())
                .andReturn();

        String notificationEventId = objectMapper.readTree(triggerResult.getResponse().getContentAsString())
                .get("notificationEventId")
                .asText();

        mockMvc.perform(get("/notifications/events/{notificationEventId}", notificationEventId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-102").claim("role", "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalOutcome").value("DELIVERED"));

        mockMvc.perform(get("/notifications/events/{notificationEventId}/attempts", notificationEventId)
                .with(jwt().jwt(jwt -> jwt.claim("sub", "notif-owner-102").claim("role", "CUSTOMER")))
                .param("page", "1")
                .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].status").value("RETRY_SCHEDULED"))
                .andExpect(jsonPath("$.items[1].status").value("SUCCEEDED"));
    }
}
