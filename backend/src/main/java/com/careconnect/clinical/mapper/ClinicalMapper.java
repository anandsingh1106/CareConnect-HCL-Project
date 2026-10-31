package com.careconnect.clinical.mapper;

import com.careconnect.clinical.dto.ClinicalDtos.DiagnosisDto;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterDto;
import com.careconnect.clinical.dto.ClinicalDtos.Icd10Dto;
import com.careconnect.clinical.dto.ClinicalDtos.NoteDto;
import com.careconnect.clinical.dto.ClinicalDtos.NoteTemplateDto;
import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.clinical.model.ClinicalNote;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.clinical.model.EncounterDiagnosis;
import com.careconnect.clinical.model.Icd10Code;
import com.careconnect.clinical.model.NoteTemplate;
import com.careconnect.clinical.model.Vital;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ClinicalMapper {

    @Mapping(target = "patientId", source = "encounter.patient.id")
    @Mapping(target = "patientName", expression = "java(encounter.getPatient().getFullName())")
    @Mapping(target = "patientMrn", source = "encounter.patient.mrn")
    @Mapping(target = "providerId", source = "encounter.provider.id")
    @Mapping(target = "providerName", source = "encounter.provider.fullName")
    @Mapping(target = "departmentName", source = "encounter.department.name")
    @Mapping(target = "appointmentId", source = "encounter.appointment.id")
    @Mapping(target = "primaryDiagnosis", source = "primaryDiagnosis")
    EncounterDto toDto(Encounter encounter, String primaryDiagnosis);

    @Mapping(target = "encounterId", source = "encounter.id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.fullName")
    @Mapping(target = "parentNoteId", source = "parentNote.id")
    @Mapping(target = "signedByName", source = "signedBy.fullName")
    NoteDto toDto(ClinicalNote note);

    @Mapping(target = "encounterId", source = "encounter.id")
    @Mapping(target = "icd10Code", source = "icd10.code")
    @Mapping(target = "description", source = "icd10.description")
    DiagnosisDto toDto(EncounterDiagnosis diagnosis);

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "encounterId", source = "encounter.id")
    @Mapping(target = "recordedById", source = "recordedBy.id")
    @Mapping(target = "recordedByName", source = "recordedBy.fullName")
    VitalDto toDto(Vital vital);

    Icd10Dto toDto(Icd10Code code);

    NoteTemplateDto toDto(NoteTemplate template);
}
