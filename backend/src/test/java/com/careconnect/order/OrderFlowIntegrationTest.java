package com.careconnect.order;

import com.careconnect.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Medication order with decision support, then pharmacist verification and dispensing. */
class OrderFlowIntegrationTest extends IntegrationTestBase {

    @Test
    void allergyAlertBlocksUntilOverriddenThenPharmacyDispenses() throws Exception {
        String doctor = bearer("dr.mehta");

        // Open an encounter for Ravi Kumar (patient 1, penicillin allergy)
        JsonNode encounter = json.readTree(mvc.perform(post("/api/encounters").header("Authorization", doctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"encounterType\":\"OUTPATIENT\",\"chiefComplaint\":\"Sore throat\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long encounterId = encounter.get("id").asLong();

        long amoxicillinId = drugId(doctor, "AMOX500");
        String order = """
                {"patientId":1,"encounterId":%d,"orderType":"MEDICATION",
                 "medication":{"drugId":%d,"dose":"500 mg","route":"ORAL","frequency":"TDS","durationDays":5,"quantity":15}%s}
                """;

        // 1. Without an override reason the order is refused and the alerts are returned
        mvc.perform(post("/api/orders").header("Authorization", doctor).contentType(MediaType.APPLICATION_JSON)
                        .content(order.formatted(encounterId, amoxicillinId, "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CDS_ALERTS"))
                .andExpect(jsonPath("$.details.alerts[*].type").value(hasItem("DRUG_ALLERGY")));

        // 2. With an override reason it is placed and the prescription waits for the pharmacist
        JsonNode placed = json.readTree(mvc.perform(post("/api/orders").header("Authorization", doctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order.formatted(encounterId, amoxicillinId,
                                ",\"overrideReason\":\"Allergy history doubtful, test dose tolerated\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.status").value("PLACED"))
                .andExpect(jsonPath("$.prescription.status").value("PENDING_VERIFICATION"))
                .andReturn().getResponse().getContentAsString());
        long rxId = placed.get("prescription").get("id").asLong();

        // 3. Nurses cannot verify; the pharmacist verifies and dispenses
        mvc.perform(post("/api/prescriptions/" + rxId + "/verify").header("Authorization", bearer("nurse.priya")))
                .andExpect(status().isForbidden());
        String pharmacist = bearer("pharm.arjun");
        mvc.perform(post("/api/prescriptions/" + rxId + "/verify").header("Authorization", pharmacist))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFIED"));
        mvc.perform(post("/api/prescriptions/" + rxId + "/dispense").header("Authorization", pharmacist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPENSED"))
                .andExpect(jsonPath("$.dispensedQty").value(15));

        mvc.perform(get("/api/orders/" + placed.get("order").get("id").asLong()).header("Authorization", doctor))
                .andExpect(jsonPath("$.order.status").value("COMPLETED"))
                .andExpect(jsonPath("$.alerts[0].overridden").value(true));
    }

    @Test
    void labResultIsFlaggedAndCompletesTheOrder() throws Exception {
        // Seeded order ORD-000009 (WBC for Sita Verma) is PLACED
        JsonNode orders = json.readTree(mvc.perform(get("/api/orders?status=PLACED&type=LAB")
                        .header("Authorization", bearer("nurse.priya")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long orderId = orders.get("content").get(0).get("id").asLong();

        mvc.perform(post("/api/orders/" + orderId + "/results").header("Authorization", bearer("nurse.priya"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valueNumeric\":1.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flag").value("CRITICAL_LOW"));
    }

    private long drugId(String token, String code) throws Exception {
        JsonNode page = json.readTree(mvc.perform(get("/api/drugs?q=" + code).header("Authorization", token))
                .andReturn().getResponse().getContentAsString());
        return page.get("content").get(0).get("id").asLong();
    }
}
