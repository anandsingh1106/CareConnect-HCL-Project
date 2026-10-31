package com.careconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application against H2 (PostgreSQL mode) with the same Liquibase scripts and demo data
 * as the real database, so tests can log in as the seeded users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Password@123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected JsonNode loginResponse(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    protected String bearer(String username) throws Exception {
        return "Bearer " + loginResponse(username).get("accessToken").asText();
    }
}
