package com.enterpriseops.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Needs Docker (starts a throw-away PostgreSQL). Run with: mvn verify */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.jwt.secret", () -> "integration-test-secret-at-least-32-characters");
        r.add("app.seed-demo", () -> "true");
        r.add("app.demo-password", () -> "Integration#Pass1");
        r.add("app.ai.enabled", () -> "false");   // AI service is not part of this test
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Integration#Pass1\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(body);
        return "Bearer " + n.get("token").asText();
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mvc.perform(get("/api/v1/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@enterpriseops.local\",\"password\":\"nope-nope\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCanCreateTicketButCannotSeeAnalytics() throws Exception {
        String token = login("employee@enterpriseops.local");
        mvc.perform(post("/api/v1/tickets").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Printer jam\",\"description\":\"Third floor printer\",\"category\":\"hardware\",\"priority\":\"LOW\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/api/v1/analytics/summary").header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void agentCanReadAnalyticsAndAdminOnlyAuditIsProtected() throws Exception {
        String agent = login("agent1@enterpriseops.local");
        mvc.perform(get("/api/v1/analytics/summary").header("Authorization", agent)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/audit-logs").header("Authorization", agent)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/audit-logs").header("Authorization", login("admin@enterpriseops.local"))).andExpect(status().isOk());
    }

    @Test
    void metricIngestionWithHighCpuCreatesOneIncidentOnly() throws Exception {
        String admin = login("admin@enterpriseops.local");
        String assets = mvc.perform(get("/api/v1/assets").header("Authorization", admin)).andReturn().getResponse().getContentAsString();
        long assetId = json.readTree(assets).get("content").get(0).get("id").asLong();
        String reading = "{\"assetId\":" + assetId + ",\"cpu\":98,\"ram\":50,\"disk\":50,\"networkMbps\":100,\"simulated\":true}";
        mvc.perform(post("/api/v1/monitoring/metrics").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(reading))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.incidentCreated").value(true));
        mvc.perform(post("/api/v1/monitoring/metrics").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(reading))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.incidentCreated").value(false));
    }
}
