package com.careconnect.patient;

import com.careconnect.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientControllerIntegrationTest extends IntegrationTestBase {

    private static final String NEW_PATIENT = """
            {"firstName":"Meena","lastName":"Joshi","dateOfBirth":"1990-05-17","gender":"FEMALE",
             "phone":"9822200011","bloodGroup":"B+","city":"Nagpur"}
            """;

    @Test
    void receptionistRegistersPatientAndGetsMrn() throws Exception {
        mvc.perform(post("/api/patients").header("Authorization", bearer("recep.neha"))
                        .contentType(MediaType.APPLICATION_JSON).content(NEW_PATIENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mrn").value(matchesPattern("CC-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.age").isNumber());
    }

    @Test
    void invalidRegistrationListsFieldErrors() throws Exception {
        mvc.perform(post("/api/patients").header("Authorization", bearer("recep.neha"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(4)));
    }

    @Test
    void searchIsPagedAndSortable() throws Exception {
        mvc.perform(get("/api/patients?q=a&page=0&size=2&sort=lastName,asc").header("Authorization", bearer("dr.mehta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void patientsCannotSearchOtherPatients() throws Exception {
        mvc.perform(get("/api/patients").header("Authorization", bearer("patient.ravi")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void receptionistCannotOpenTheClinicalChart() throws Exception {
        mvc.perform(get("/api/patients/1/summary").header("Authorization", bearer("recep.neha")))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorSeesChartSummaryWithAllergies() throws Exception {
        mvc.perform(get("/api/patients/1/summary").header("Authorization", bearer("dr.mehta")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patient.mrn").value("CC-2026-000001"))
                .andExpect(jsonPath("$.allergies[0].allergen").value("Penicillin"));
    }
}
