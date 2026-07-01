package com.example.banking.api.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api-docs-test-db;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class ApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocsEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").isString())
            .andExpect(jsonPath("$.info.title").value("NorthBridge Banking API"))
            .andExpect(jsonPath("$.paths['/accounts'].get.summary").value("List accounts"))
            .andExpect(jsonPath("$.paths['/accounts'].get.description").value(
                "Returns a paginated, filterable list of accounts for the selected customer scope."))
            .andExpect(jsonPath("$.paths['/notifications/events'].post.summary").value("Trigger notification event"))
            .andExpect(jsonPath("$.paths['/notifications/events'].post.description").value(
                "Creates a notification event for dispatch processing and returns the accepted event reference."));
    }

    @Test
    void swaggerUiEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
