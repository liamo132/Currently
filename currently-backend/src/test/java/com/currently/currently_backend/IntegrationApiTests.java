package com.currently.currently_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Verifies a newly registered account can log in and receive a usable token.
    @Test
    void authFlowSupportsRegisterAndLogin() throws Exception {
        TestAccount account = registerAccount("login");

        String loginBody = """
                {
                  "email": "%s",
                  "password": "Password123!"
                }
                """.formatted(account.email());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        String loginToken = readJson(loginResult).get("token").asText();
        assertThat(loginToken).isNotBlank();
    }

    // Verifies protected endpoints reject anonymous access and allow authenticated access.
    @Test
    void authFlowAndProtectedEndpointEnforcement() throws Exception {
        mockMvc.perform(get("/api/users/me/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String token = registerAndGetToken();
        mockMvc.perform(get("/api/users/me/rooms")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Verifies invalid registration input returns the shared validation error contract.
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

    // Verifies login fails with the expected error when credentials are incorrect.
    @Test
    void authLoginRejectsBadCredentials() throws Exception {
        TestAccount account = registerAccount("badcreds");

        String body = """
                {
                  "email": "%s",
                  "password": "WrongPass123!"
                }
                """.formatted(account.email());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // Verifies duplicate registration attempts are rejected with the expected conflict message.
    @Test
    void authDuplicateRegistrationRejectedWithConflictMessage() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String registerBody = """
                {
                  "username": "existing-%s",
                  "name": "Duplicate Test",
                  "email": "existing-%s@example.com",
                  "password": "Password123!"
                }
                """.formatted(unique, unique);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody.replace("existing-%s", "other-user"))
                        .content(String.format(
                                """
                                {
                                  "username": "other-user-%s",
                                  "name": "Duplicate Test",
                                  "email": "existing-%s@example.com",
                                  "password": "Password123!"
                                }
                                """,
                                unique, unique))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Email already in use."));
    }

    // Verifies the authenticated room API supports create, list, update, and delete operations.
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
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kitchen"))
                .andReturn();

        Long roomId = readJson(createResult).get("id").asLong();

        mockMvc.perform(get("/api/users/me/rooms")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kitchen"));

        String updateBody = """
                {
                  "name": "Updated Kitchen",
                  "floorLabel": "Ground"
                }
                """;

        mockMvc.perform(put("/api/users/me/rooms/{id}", roomId)
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Kitchen"));

        mockMvc.perform(delete("/api/users/me/rooms/{id}", roomId)
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // Verifies one user cannot delete a room created by another user.
    @Test
    void roomsRejectCrossUserDelete() throws Exception {
        String ownerToken = registerAndGetToken();

        MvcResult createResult = mockMvc.perform(post("/api/users/me/rooms")
                        .header(AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bedroom",
                                  "floorLabel": "Upper",
                                  "type": "Bedroom"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Long roomId = readJson(createResult).get("id").asLong();
        String intruderToken = registerAndGetToken();

        mockMvc.perform(delete("/api/users/me/rooms/{id}", roomId)
                        .header(AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // Verifies the appliance catalogue endpoint is intentionally public.
    @Test
    void appliancesApiIsPublic() throws Exception {
        mockMvc.perform(get("/api/appliances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // Verifies appliance creation feeds insight generation and pagination end to end.
    @Test
    void appliancesAndInsightsIntegrationJourney() throws Exception {
        String token = registerAndGetToken();
        String applianceBody = """
                {
                  "applianceName": "Electric Heater",
                  "customName": "Office Heater",
                  "usageType": "continuous",
                  "hoursPerDay": 4
                }
                """;

        mockMvc.perform(post("/api/users/me/appliances")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applianceBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyKWh").isNumber());

        MvcResult generateResult = mockMvc.perform(post("/api/insights/generate")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isString())
                .andExpect(jsonPath("$.insights").isArray())
                .andReturn();

        JsonNode generateJson = readJson(generateResult);
        assertThat(generateJson.get("insights").size()).isGreaterThan(0);
        assertThat(generateJson.get("insights").get(0).get("impactMonthly").asDouble()).isGreaterThan(0);

        String runId = generateJson.get("runId").asText();
        mockMvc.perform(post("/api/insights/{runId}/more", runId)
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isArray());
    }

    // Verifies the main user journey works from room creation through insight generation.
    @Test
    void endToEndUserJourneyCreateRoomAddApplianceAndGenerateInsights() throws Exception {
        String token = registerAndGetToken();

        MvcResult roomResult = mockMvc.perform(post("/api/users/me/rooms")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Office",
                                  "floorLabel": "Ground",
                                  "type": "Office"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Long roomId = readJson(roomResult).get("id").asLong();

        mockMvc.perform(post("/api/users/me/appliances")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applianceName": "Electric Heater",
                                  "usageType": "continuous",
                                  "hoursPerDay": 4,
                                  "roomId": %d
                                }
                                """.formatted(roomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.dailyKWh").value(8.0))
                .andExpect(jsonPath("$.estimatedDailyCost").value(2.4));

        MvcResult generateResult = mockMvc.perform(post("/api/insights/generate")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isArray())
                .andReturn();

        String runId = readJson(generateResult).get("runId").asText();
        mockMvc.perform(post("/api/insights/{runId}/more", runId)
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isArray());
    }

    // Verifies insight run pagination cannot be accessed by a different authenticated user.
    @Test
    void appliancesInsightRunCanBeAccessedOnlyByOwningUser() throws Exception {
        String ownerToken = registerAndGetToken();
        mockMvc.perform(post("/api/users/me/appliances")
                        .header(AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applianceName": "Electric Heater",
                                  "usageType": "continuous",
                                  "hoursPerDay": 4
                                }
                                """))
                .andExpect(status().isOk());

        String intruderToken = registerAndGetToken();
        MvcResult generateResult = mockMvc.perform(post("/api/insights/generate")
                        .header(AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isString())
                .andReturn();

        String runId = readJson(generateResult).get("runId").asText();
        mockMvc.perform(post("/api/insights/{runId}/more", runId)
                        .header(AUTHORIZATION, "Bearer " + intruderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // Verifies energy settings can be retrieved and updated through the API.
    @Test
    void energySettingsCanBeSavedAndLoaded() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/users/me/energy-settings")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricePerKwh").doesNotExist());

        String updateBody = """
                {
                  "pricePerKwh": 0.38,
                  "providerName": "Sample Utility"
                }
                """;

        mockMvc.perform(put("/api/users/me/energy-settings")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricePerKwh").value(0.38))
                .andExpect(jsonPath("$.providerName").value("Sample Utility"));
    }

    // Verifies invalid appliance payloads return the shared validation error contract.
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
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.usageType").exists());
    }

    // Verifies insight generation returns a no-data response before appliances exist.
    @Test
    void insightsRequireApplianceDataBeforeGeneration() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/insights/generate")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isEmpty())
                .andExpect(jsonPath("$.stopReason").value("No appliance data available yet."));
    }

    private String registerAndGetToken() throws Exception {
        return registerAccount("journey").token();
    }

    private TestAccount registerAccount(String prefix) throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = String.format("%s-%s@example.com", prefix, unique);
        String username = String.format("%s-%s", prefix, unique);

        String registerBody = """
                {
                  "username": "%s",
                  "name": "Integration Test",
                  "email": "%s",
                  "password": "Password123!"
                }
                """.formatted(username, email);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        return new TestAccount(username, email, "Password123!", readJson(result).get("token").asText());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record TestAccount(String username, String email, String password, String token) {}
}
