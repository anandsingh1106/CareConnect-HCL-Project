package com.careconnect.portal.dto;

import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.EncounterType;

import java.time.LocalDateTime;
import java.util.List;

public final class PortalDtos {

    private PortalDtos() {
    }

    /** What a patient sees about one visit: diagnoses, the doctor's signed assessment/plan and medicines. */
    public record VisitSummaryDto(
            Long encounterId,
            LocalDateTime encounterDate,
            String providerName,
            String departmentName,
            EncounterType encounterType,
            EncounterStatus status,
            String chiefComplaint,
            List<String> diagnoses,
            String assessment,
            String plan,
            List<String> medications,
            VitalDto vitals) {
    }
}
