package com.careconnect.clinical.dto;

import com.careconnect.clinical.model.ClinicalEnums.DiagnosisType;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.EncounterType;
import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for clinical documentation. */
public final class ClinicalDtos {

    private ClinicalDtos() {
    }

    public record EncounterDto(
            Long id,
            Long patientId,
            String patientName,
            String patientMrn,
            Long providerId,
            String providerName,
            String departmentName,
            Long appointmentId,
            EncounterType encounterType,
            EncounterStatus status,
            LocalDateTime encounterDate,
            String chiefComplaint,
            LocalDateTime closedAt,
            String primaryDiagnosis,
            LocalDateTime createdAt) {
    }

    public record EncounterRequest(
            @NotNull Long patientId,
            /** Doctor responsible for the visit. Optional when a doctor opens the encounter (defaults to self). */
            Long providerId,
            @NotNull EncounterType encounterType,
            @Size(max = 500) String chiefComplaint,
            Long appointmentId) {
    }

    public record EncounterDetailDto(
            EncounterDto encounter,
            List<NoteDto> notes,
            List<DiagnosisDto> diagnoses,
            List<VitalDto> vitals) {
    }

    public record NoteDto(
            Long id,
            Long encounterId,
            Long patientId,
            Long authorId,
            String authorName,
            NoteType noteType,
            Long parentNoteId,
            String templateCode,
            String subjective,
            String objective,
            String assessment,
            String plan,
            NoteStatus status,
            LocalDateTime signedAt,
            String signedByName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Integer version) {
    }

    public record NoteRequest(
            @Size(max = 30) String templateCode,
            @Size(max = 4000) String subjective,
            @Size(max = 4000) String objective,
            @Size(max = 4000) String assessment,
            @Size(max = 4000) String plan,
            Integer version) {
    }

    public record DiagnosisDto(Long id, Long encounterId, String icd10Code, String description,
                               DiagnosisType diagnosisType, String notes) {
    }

    public record DiagnosisRequest(
            @NotBlank @Size(max = 10) String icd10Code,
            DiagnosisType diagnosisType,
            @Size(max = 500) String notes) {
    }

    public record VitalDto(
            Long id,
            Long patientId,
            Long encounterId,
            Long recordedById,
            String recordedByName,
            LocalDateTime recordedAt,
            Integer systolic,
            Integer diastolic,
            Integer pulse,
            BigDecimal temperature,
            Integer respRate,
            Integer spo2,
            BigDecimal weightKg,
            BigDecimal heightCm,
            BigDecimal bmi,
            String notes) {
    }

    /** Physiologically plausible ranges reject typing mistakes such as a systolic of 1200. */
    public record VitalRequest(
            Long encounterId,
            @PastOrPresent LocalDateTime recordedAt,
            @Min(50) @Max(260) Integer systolic,
            @Min(30) @Max(160) Integer diastolic,
            @Min(20) @Max(250) Integer pulse,
            @DecimalMin("30.0") @DecimalMax("45.0") BigDecimal temperature,
            @Min(4) @Max(60) Integer respRate,
            @Min(50) @Max(100) Integer spo2,
            @DecimalMin("0.5") @DecimalMax("400.0") BigDecimal weightKg,
            @DecimalMin("30.0") @DecimalMax("250.0") BigDecimal heightCm,
            @Size(max = 500) String notes) {
    }

    public record Icd10Dto(String code, String description, String category, boolean active) {
    }

    public record Icd10Request(
            @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Z][0-9][0-9A-Z](\\.[0-9A-Z]{1,4})?$",
                    message = "Use ICD-10 format, e.g. E11.9") String code,
            @NotBlank @Size(max = 255) String description,
            @Size(max = 100) String category,
            boolean active) {
    }

    public record NoteTemplateDto(Long id, String code, String name, String specialty, String subjective,
                                  String objective, String assessment, String plan) {
    }
}
