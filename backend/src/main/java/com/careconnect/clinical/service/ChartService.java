package com.careconnect.clinical.service;

import com.careconnect.clinical.dto.ClinicalDtos.EncounterDto;
import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.clinical.repository.VitalRepository;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.patient.dto.PatientDtos.AllergyDto;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.patient.dto.PatientDtos.ProblemDto;
import com.careconnect.patient.mapper.PatientMapper;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.model.PatientEnums.ProblemStatus;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import com.careconnect.patient.repository.AllergyRepository;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.patient.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The header of the patient chart: demographics plus the safety-critical facts a clinician must see first. */
@Service
@RequiredArgsConstructor
public class ChartService {

    public record ChartSummaryDto(
            PatientDto patient,
            List<AllergyDto> allergies,
            List<ProblemDto> problems,
            List<PrescriptionDto> activeMedications,
            VitalDto latestVitals,
            List<EncounterDto> openEncounters) {
    }

    private final PatientRepository patients;
    private final AllergyRepository allergies;
    private final ProblemRepository problems;
    private final PrescriptionRepository prescriptions;
    private final VitalRepository vitals;
    private final EncounterRepository encounters;
    private final PatientMapper patientMapper;
    private final ClinicalMapper clinicalMapper;
    private final MedicationMapper medicationMapper;

    @Transactional(readOnly = true)
    public ChartSummaryDto summary(Long patientId) {
        Patient patient = patients.require(patientId);
        return new ChartSummaryDto(
                patientMapper.toDto(patient),
                allergies.findByPatientIdAndStatus(patientId, RecordStatus.ACTIVE).stream().map(patientMapper::toDto).toList(),
                problems.findByPatientIdAndStatus(patientId, ProblemStatus.ACTIVE).stream().map(patientMapper::toDto).toList(),
                prescriptions.findByPatientIdAndStatusIn(patientId, PrescriptionStatus.OPEN).stream()
                        .filter(Prescription::isCurrentlyActive).map(medicationMapper::toDto).toList(),
                vitals.findFirstByPatientIdOrderByRecordedAtDesc(patientId).map(clinicalMapper::toDto).orElse(null),
                encounters.findByPatientIdAndStatusOrderByEncounterDateDesc(patientId, EncounterStatus.OPEN).stream()
                        .map(e -> clinicalMapper.toDto(e, null)).toList());
    }
}
