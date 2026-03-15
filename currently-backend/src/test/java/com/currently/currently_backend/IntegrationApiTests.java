package com.currently.currently_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authFlowAndProtectedEndpointEnforcement() throws Exception {
        mockMvc.perform(get("/api/users/me/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String token = registerAndGetToken();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/users/me/rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void authValidationUsesStandardErrorShape() throws Exception {
        String body = """
                {
                  "username": "",
                  "email": "bad-email",
                  "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void roomsApiCrudJourney() throws Exception {
        String token = registerAndGetToken();

        String createBody = """
                {
                  "name": "Kitchen",
                  "floorLabel": "Ground",
                  "type": "Kitchen"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/users/me/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kitchen"))
                .andReturn();

        Long roomId = readJson(createResult).get("id").asLong();

        mockMvc.perform(get("/api/users/me/rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kitchen"));

        String updateBody = """
                {
                  "name": "Updated Kitchen",
                  "floorLabel": "Ground"
                }
                """;

        mockMvc.perform(put("/api/users/me/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Kitchen"));

        mockMvc.perform(delete("/api/users/me/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void appliancesApiIsPublic() throws Exception {
        mockMvc.perform(get("/api/appliances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void appliancesAndInsightsIntegrationJourney() throws Exception {
        String token = registerAndGetToken();

        String applianceBody = """
                {
                  "applianceName": "Fridge",
                  "customName": "Kitchen Fridge",
                  "usageType": "continuous",
                  "hoursPerDay": 24
                }
                """;

        mockMvc.perform(post("/api/users/me/appliances")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applianceBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyKWh").isNumber());

        String generateBody = """
                {
                  "pricePerKwh": 0.35
                }
                """;

        MvcResult generateResult = mockMvc.perform(post("/api/insights/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isString())
                .andExpect(jsonPath("$.insights").isArray())
                .andReturn();

        String runId = readJson(generateResult).get("runId").asText();

        mockMvc.perform(post("/api/insights/{runId}/more", runId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isArray());
    }

    @Test
    void applianceValidationRejectedWithStandardError() throws Exception {
        String token = registerAndGetToken();
        String invalidBody = """
                {
                  "applianceName": "Fridge",
                  "usageType": "invalid",
                  "hoursPerDay": -1
                }
                """;

        mockMvc.perform(post("/api/users/me/appliances")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.usageType").exists());
    }

    private String registerAndGetToken() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String registerBody = """
                {
                  "username": "user-%s",
                  "name": "Integration Test",
                  "email": "user-%s@example.com",
                  "password": "Password123!"
                }
                """.formatted(unique, unique);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        return readJson(result).get("token").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
