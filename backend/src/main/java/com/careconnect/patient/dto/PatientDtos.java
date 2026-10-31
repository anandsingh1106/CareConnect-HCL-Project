package com.careconnect.patient.dto;

import com.careconnect.patient.model.PatientEnums.AllergySeverity;
import com.careconnect.patient.model.PatientEnums.AllergyType;
import com.careconnect.patient.model.PatientEnums.DocumentCategory;
import com.careconnect.patient.model.PatientEnums.Gender;
import com.careconnect.patient.model.PatientEnums.ProblemStatus;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Request/response bodies for patient records. */
public final class PatientDtos {

    private PatientDtos() {
    }

    public static final String PHONE_REGEX = "^[0-9+][0-9 -]{6,19}$";

    /** Full demographics (staff and the patient themselves). */
    public record PatientDto(
            Long id,
            String mrn,
            String firstName,
            String lastName,
            String fullName,
            LocalDate dateOfBirth,
            Integer age,
            Gender gender,
            String bloodGroup,
            String phone,
            String email,
            String addressLine,
            String city,
            String stateName,
            String postalCode,
            String emergencyName,
            String emergencyPhone,
            String emergencyRelation,
            String insuranceProvider,
            String insurancePolicyNo,
            LocalDate insuranceValidTill,
            String medicalHistory,
            Long primaryProviderId,
            String primaryProviderName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Integer version) {
    }

    /** Row in the patient search result. */
    public record PatientSummaryDto(
            Long id,
            String mrn,
            String fullName,
            LocalDate dateOfBirth,
            Integer age,
            Gender gender,
            String phone,
            String city,
            String primaryProviderName) {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Use A+, A-, B+, B-, AB+, AB-, O+ or O-.") String bloodGroup,
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number.") String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 200) String addressLine,
            @Size(max = 60) String city,
            @Size(max = 60) String stateName,
            @Size(max = 12) String postalCode,
            @Size(max = 120) String emergencyName,
            @Pattern(regexp = PHONE_REGEX, message = "Enter a valid phone number.") String emergencyPhone,
            @Size(max = 40) String emergencyRelation,
            @Size(max = 100) String insuranceProvider,
            @Size(max = 50) String insurancePolicyNo,
            LocalDate insuranceValidTill,
            @Size(max = 4000) String medicalHistory,
            Long primaryProviderId,
            /** Version the client last saw; if it differs the update is rejected (someone else changed it). */
            Integer version) {
    }

    public record AllergyDto(Long id, Long patientId, String allergen, AllergyType allergyType, String reaction,
                             AllergySeverity severity, RecordStatus status, LocalDate notedOn,
                             String createdBy, LocalDateTime createdAt) {
    }

    public record AllergyRequest(
            @NotBlank @Size(max = 100) String allergen,
            @NotNull AllergyType allergyType,
            @Size(max = 200) String reaction,
            @NotNull AllergySeverity severity,
            RecordStatus status,
            @PastOrPresent LocalDate notedOn) {
    }

    public record ProblemDto(Long id, Long patientId, String icd10Code, String icd10Description, String description,
                             ProblemStatus status, LocalDate onsetDate, LocalDate resolvedDate,
                             String createdBy, LocalDateTime createdAt) {
    }

    public record ProblemRequest(
            @Size(max = 10) String icd10Code,
            @NotBlank @Size(max = 255) String description,
            ProblemStatus status,
            @PastOrPresent LocalDate onsetDate,
            @PastOrPresent LocalDate resolvedDate) {
    }

    public record ImmunizationDto(Long id, Long patientId, String vaccineName, Integer doseNumber,
                                  LocalDate administeredOn, String administeredBy, String lotNumber, String notes,
                                  String createdBy, LocalDateTime createdAt) {
    }

    public record ImmunizationRequest(
            @NotBlank @Size(max = 120) String vaccineName,
            @Min(1) @Max(20) Integer doseNumber,
            @NotNull @PastOrPresent LocalDate administeredOn,
            @Size(max = 120) String administeredBy,
            @Size(max = 40) String lotNumber,
            @Size(max = 500) String notes) {
    }

    public record FamilyHistoryDto(Long id, Long patientId, String relation, String conditionName, String notes,
                                   String createdBy, LocalDateTime createdAt) {
    }

    public record FamilyHistoryRequest(
            @NotBlank @Size(max = 40) String relation,
            @NotBlank @Size(max = 200) String conditionName,
            @Size(max = 500) String notes) {
    }

    public record DocumentDto(Long id, Long patientId, DocumentCategory category, String fileName, String contentType,
                              Long sizeBytes, String description, String uploadedBy, LocalDateTime uploadedAt) {
    }
}
