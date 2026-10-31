package com.careconnect.auth;

import com.careconnect.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void loginReturnsTokensAndProfile() throws Exception {
        JsonNode response = loginResponse("dr.mehta");
        org.assertj.core.api.Assertions.assertThat(response.get("accessToken").asText()).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(response.get("user").get("role").asText()).isEqualTo("DOCTOR");
    }

    @Test
    void wrongPasswordIsRejectedWithGenericMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dr.mehta\",\"password\":\"nope1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointNeedsAToken() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void refreshRotatesAndRejectsReuse() throws Exception {
        String refresh = loginResponse("nurse.priya").get("refreshToken").asText();
        String body = "{\"refreshToken\":\"" + refresh + "\"}";

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer("patient.ravi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.patientId").isNumber());
    }
}
