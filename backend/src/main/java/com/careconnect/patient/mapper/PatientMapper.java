package com.careconnect.patient.mapper;

import com.careconnect.patient.dto.PatientDtos.AllergyDto;
import com.careconnect.patient.dto.PatientDtos.DocumentDto;
import com.careconnect.patient.dto.PatientDtos.FamilyHistoryDto;
import com.careconnect.patient.dto.PatientDtos.ImmunizationDto;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.patient.dto.PatientDtos.PatientRequest;
import com.careconnect.patient.dto.PatientDtos.PatientSummaryDto;
import com.careconnect.patient.dto.PatientDtos.ProblemDto;
import com.careconnect.patient.model.Allergy;
import com.careconnect.patient.model.FamilyHistory;
import com.careconnect.patient.model.Immunization;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.model.PatientDocument;
import com.careconnect.patient.model.Problem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;
import java.time.Period;

@Mapper
public interface PatientMapper {

    @Mapping(target = "fullName", expression = "java(patient.getFullName())")
    @Mapping(target = "age", expression = "java(ageOf(patient.getDateOfBirth()))")
    @Mapping(target = "primaryProviderId", source = "primaryProvider.id")
    @Mapping(target = "primaryProviderName", source = "primaryProvider.fullName")
    PatientDto toDto(Patient patient);

    @Mapping(target = "fullName", expression = "java(patient.getFullName())")
    @Mapping(target = "age", expression = "java(ageOf(patient.getDateOfBirth()))")
    @Mapping(target = "primaryProviderName", source = "primaryProvider.fullName")
    PatientSummaryDto toSummary(Patient patient);

    /** Copies editable demographics onto the entity. Identity, MRN, audit and version fields are never copied. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mrn", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "primaryProvider", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(PatientRequest request, @MappingTarget Patient patient);

    @Mapping(target = "patientId", source = "patient.id")
    AllergyDto toDto(Allergy allergy);

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "icd10Code", source = "icd10.code")
    @Mapping(target = "icd10Description", source = "icd10.description")
    ProblemDto toDto(Problem problem);

    @Mapping(target = "patientId", source = "patient.id")
    ImmunizationDto toDto(Immunization immunization);

    @Mapping(target = "patientId", source = "patient.id")
    FamilyHistoryDto toDto(FamilyHistory familyHistory);

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "uploadedBy", source = "createdBy")
    @Mapping(target = "uploadedAt", source = "createdAt")
    DocumentDto toDto(PatientDocument document);

    default Integer ageOf(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
