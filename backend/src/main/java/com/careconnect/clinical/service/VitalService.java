package com.careconnect.clinical.service;

import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.clinical.dto.ClinicalDtos.VitalRequest;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.clinical.model.Vital;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.clinical.repository.VitalRepository;
import com.careconnect.common.exception.ApiException;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class VitalService {

    private final VitalRepository vitals;
    private final PatientRepository patients;
    private final EncounterRepository encounters;
    private final AppUserRepository users;
    private final ClinicalMapper mapper;

    @Transactional
    public VitalDto record(Long patientId, VitalRequest request) {
        Patient patient = patients.require(patientId);
        boolean nothingMeasured = Stream.of(request.systolic(), request.diastolic(), request.pulse(), request.temperature(),
                request.respRate(), request.spo2(), request.weightKg(), request.heightCm()).allMatch(v -> v == null);
        if (nothingMeasured) {
            throw ApiException.badRequest("Enter at least one measurement.");
        }
        if ((request.systolic() == null) != (request.diastolic() == null)) {
            throw ApiException.badRequest("Enter both systolic and diastolic blood pressure.");
        }
        if (request.systolic() != null && request.systolic() <= request.diastolic()) {
            throw ApiException.badRequest("Systolic pressure must be higher than diastolic pressure.");
        }

        Vital vital = new Vital();
        vital.setPatient(patient);
        if (request.encounterId() != null) {
            Encounter encounter = encounters.findById(request.encounterId())
                    .orElseThrow(() -> ApiException.notFound("Encounter", request.encounterId()));
            if (!encounter.getPatient().getId().equals(patientId)) {
                throw ApiException.badRequest("The encounter belongs to a different patient.");
            }
            if (encounter.getStatus() != EncounterStatus.OPEN) {
                throw ApiException.conflict("ENCOUNTER_CLOSED", "Vitals cannot be added to a closed encounter.");
            }
            vital.setEncounter(encounter);
        }
        Long userId = SecurityUtils.currentUser().id();
        vital.setRecordedBy(users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId)));
        vital.setRecordedAt(request.recordedAt() == null ? LocalDateTime.now() : request.recordedAt());
        vital.setSystolic(request.systolic());
        vital.setDiastolic(request.diastolic());
        vital.setPulse(request.pulse());
        vital.setTemperature(request.temperature());
        vital.setRespRate(request.respRate());
        vital.setSpo2(request.spo2());
        vital.setWeightKg(request.weightKg());
        vital.setHeightCm(request.heightCm());
        vital.setNotes(request.notes());

        // Height rarely changes for adults, so reuse the last known height if it was not measured today.
        BigDecimal height = request.heightCm() != null ? request.heightCm()
                : vitals.findFirstByPatientIdAndHeightCmIsNotNullOrderByRecordedAtDesc(patientId)
                        .map(Vital::getHeightCm).orElse(null);
        vital.setBmi(calculateBmi(request.weightKg(), height));
        return mapper.toDto(vitals.save(vital));
    }

    @Transactional(readOnly = true)
    public List<VitalDto> history(Long patientId, LocalDate from, LocalDate to) {
        patients.require(patientId);
        List<Vital> list = (from == null && to == null)
                ? vitals.findByPatientIdOrderByRecordedAtAsc(patientId)
                : vitals.findByPatientIdAndRecordedAtBetweenOrderByRecordedAtAsc(patientId,
                        from == null ? LocalDate.of(1900, 1, 1).atStartOfDay() : from.atStartOfDay(),
                        to == null ? LocalDateTime.now().plusDays(1) : to.plusDays(1).atStartOfDay());
        return list.stream().map(mapper::toDto).toList();
    }

    /** BMI = weight (kg) / height (m)^2, rounded to one decimal. */
    public static BigDecimal calculateBmi(BigDecimal weightKg, BigDecimal heightCm) {
        if (weightKg == null || heightCm == null || heightCm.signum() <= 0) {
            return null;
        }
        BigDecimal meters = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(meters.multiply(meters), 1, RoundingMode.HALF_UP);
    }
}
