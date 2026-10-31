package com.careconnect.patient.service;

import com.careconnect.clinical.model.Icd10Code;
import com.careconnect.clinical.repository.Icd10Repository;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.patient.dto.PatientDtos.AllergyDto;
import com.careconnect.patient.dto.PatientDtos.AllergyRequest;
import com.careconnect.patient.dto.PatientDtos.FamilyHistoryDto;
import com.careconnect.patient.dto.PatientDtos.FamilyHistoryRequest;
import com.careconnect.patient.dto.PatientDtos.ImmunizationDto;
import com.careconnect.patient.dto.PatientDtos.ImmunizationRequest;
import com.careconnect.patient.dto.PatientDtos.ProblemDto;
import com.careconnect.patient.dto.PatientDtos.ProblemRequest;
import com.careconnect.patient.mapper.PatientMapper;
import com.careconnect.patient.model.Allergy;
import com.careconnect.patient.model.FamilyHistory;
import com.careconnect.patient.model.Immunization;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.model.PatientEnums.ProblemStatus;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import com.careconnect.patient.model.Problem;
import com.careconnect.patient.repository.AllergyRepository;
import com.careconnect.patient.repository.FamilyHistoryRepository;
import com.careconnect.patient.repository.ImmunizationRepository;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.patient.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Allergies, problem list, immunizations and family history. Entries are corrected or inactivated, never deleted. */
@Service
@RequiredArgsConstructor
public class PatientHistoryService {

    private final PatientRepository patients;
    private final AllergyRepository allergies;
    private final ProblemRepository problems;
    private final ImmunizationRepository immunizations;
    private final FamilyHistoryRepository familyHistory;
    private final Icd10Repository icd10;
    private final PatientMapper mapper;

    // ----- allergies -----

    @Transactional(readOnly = true)
    public List<AllergyDto> allergies(Long patientId) {
        patients.require(patientId);
        return allergies.findByPatientIdOrderByStatusAscNotedOnDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public AllergyDto addAllergy(Long patientId, AllergyRequest request) {
        Allergy allergy = new Allergy();
        allergy.setPatient(patients.require(patientId));
        applyAllergy(allergy, request);
        return mapper.toDto(allergies.save(allergy));
    }

    @Transactional
    public AllergyDto updateAllergy(Long allergyId, AllergyRequest request) {
        Allergy allergy = allergies.findById(allergyId).orElseThrow(() -> ApiException.notFound("Allergy", allergyId));
        AuditContext.setPatientId(allergy.getPatient().getId());
        applyAllergy(allergy, request);
        return mapper.toDto(allergy);
    }

    private void applyAllergy(Allergy allergy, AllergyRequest request) {
        allergy.setAllergen(request.allergen().trim());
        allergy.setAllergyType(request.allergyType());
        allergy.setReaction(request.reaction());
        allergy.setSeverity(request.severity());
        allergy.setStatus(request.status() == null ? RecordStatus.ACTIVE : request.status());
        allergy.setNotedOn(request.notedOn());
    }

    // ----- problem list -----

    @Transactional(readOnly = true)
    public List<ProblemDto> problems(Long patientId) {
        patients.require(patientId);
        return problems.findByPatientIdOrderByStatusAscOnsetDateDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public ProblemDto addProblem(Long patientId, ProblemRequest request) {
        Problem problem = new Problem();
        problem.setPatient(patients.require(patientId));
        applyProblem(problem, request);
        return mapper.toDto(problems.save(problem));
    }

    @Transactional
    public ProblemDto updateProblem(Long problemId, ProblemRequest request) {
        Problem problem = problems.findById(problemId).orElseThrow(() -> ApiException.notFound("Problem", problemId));
        AuditContext.setPatientId(problem.getPatient().getId());
        applyProblem(problem, request);
        return mapper.toDto(problem);
    }

    private void applyProblem(Problem problem, ProblemRequest request) {
        Icd10Code code = null;
        if (request.icd10Code() != null && !request.icd10Code().isBlank()) {
            code = icd10.findById(request.icd10Code().trim().toUpperCase())
                    .orElseThrow(() -> ApiException.badRequest("Unknown ICD-10 code " + request.icd10Code()));
        }
        problem.setIcd10(code);
        problem.setDescription(request.description().trim());
        problem.setStatus(request.status() == null ? ProblemStatus.ACTIVE : request.status());
        problem.setOnsetDate(request.onsetDate());
        problem.setResolvedDate(problem.getStatus() == ProblemStatus.RESOLVED ? request.resolvedDate() : null);
        if (problem.getOnsetDate() != null && problem.getResolvedDate() != null
                && problem.getResolvedDate().isBefore(problem.getOnsetDate())) {
            throw ApiException.badRequest("Resolved date cannot be before the onset date.");
        }
    }

    // ----- immunizations -----

    @Transactional(readOnly = true)
    public List<ImmunizationDto> immunizations(Long patientId) {
        patients.require(patientId);
        return immunizations.findByPatientIdOrderByAdministeredOnDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public ImmunizationDto addImmunization(Long patientId, ImmunizationRequest request) {
        Immunization immunization = new Immunization();
        immunization.setPatient(patients.require(patientId));
        applyImmunization(immunization, request);
        return mapper.toDto(immunizations.save(immunization));
    }

    @Transactional
    public ImmunizationDto updateImmunization(Long id, ImmunizationRequest request) {
        Immunization immunization = immunizations.findById(id).orElseThrow(() -> ApiException.notFound("Immunization", id));
        AuditContext.setPatientId(immunization.getPatient().getId());
        applyImmunization(immunization, request);
        return mapper.toDto(immunization);
    }

    private void applyImmunization(Immunization immunization, ImmunizationRequest request) {
        immunization.setVaccineName(request.vaccineName().trim());
        immunization.setDoseNumber(request.doseNumber());
        immunization.setAdministeredOn(request.administeredOn());
        immunization.setAdministeredBy(request.administeredBy());
        immunization.setLotNumber(request.lotNumber());
        immunization.setNotes(request.notes());
    }

    // ----- family history -----

    @Transactional(readOnly = true)
    public List<FamilyHistoryDto> familyHistory(Long patientId) {
        patients.require(patientId);
        return familyHistory.findByPatientIdOrderByRelationAsc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public FamilyHistoryDto addFamilyHistory(Long patientId, FamilyHistoryRequest request) {
        Patient patient = patients.require(patientId);
        FamilyHistory entry = new FamilyHistory();
        entry.setPatient(patient);
        applyFamilyHistory(entry, request);
        return mapper.toDto(familyHistory.save(entry));
    }

    @Transactional
    public FamilyHistoryDto updateFamilyHistory(Long id, FamilyHistoryRequest request) {
        FamilyHistory entry = familyHistory.findById(id).orElseThrow(() -> ApiException.notFound("Family history entry", id));
        AuditContext.setPatientId(entry.getPatient().getId());
        applyFamilyHistory(entry, request);
        return mapper.toDto(entry);
    }

    private void applyFamilyHistory(FamilyHistory entry, FamilyHistoryRequest request) {
        entry.setRelation(request.relation().trim());
        entry.setConditionName(request.conditionName().trim());
        entry.setNotes(request.notes());
    }
}
