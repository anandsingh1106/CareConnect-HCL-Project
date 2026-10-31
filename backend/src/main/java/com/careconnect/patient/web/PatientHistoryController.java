package com.careconnect.patient.web;

import com.careconnect.patient.dto.PatientDtos.AllergyDto;
import com.careconnect.patient.dto.PatientDtos.AllergyRequest;
import com.careconnect.patient.dto.PatientDtos.FamilyHistoryDto;
import com.careconnect.patient.dto.PatientDtos.FamilyHistoryRequest;
import com.careconnect.patient.dto.PatientDtos.ImmunizationDto;
import com.careconnect.patient.dto.PatientDtos.ImmunizationRequest;
import com.careconnect.patient.dto.PatientDtos.ProblemDto;
import com.careconnect.patient.dto.PatientDtos.ProblemRequest;
import com.careconnect.patient.service.PatientHistoryService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Patient history")
@RestController
@RequiredArgsConstructor
public class PatientHistoryController {

    private final PatientHistoryService service;

    // ----- allergies: pharmacists read them for dispensing safety -----

    @GetMapping("/api/patients/{patientId}/allergies")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<AllergyDto> allergies(@PathVariable Long patientId) {
        return service.allergies(patientId);
    }

    @PostMapping("/api/patients/{patientId}/allergies")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public AllergyDto addAllergy(@PathVariable Long patientId, @Valid @RequestBody AllergyRequest request) {
        return service.addAllergy(patientId, request);
    }

    @PutMapping("/api/allergies/{id}")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    public AllergyDto updateAllergy(@PathVariable Long id, @Valid @RequestBody AllergyRequest request) {
        return service.updateAllergy(id, request);
    }

    // ----- problem list -----

    @GetMapping("/api/patients/{patientId}/problems")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<ProblemDto> problems(@PathVariable Long patientId) {
        return service.problems(patientId);
    }

    @PostMapping("/api/patients/{patientId}/problems")
    @PreAuthorize(Authz.DOCTOR)
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemDto addProblem(@PathVariable Long patientId, @Valid @RequestBody ProblemRequest request) {
        return service.addProblem(patientId, request);
    }

    @PutMapping("/api/problems/{id}")
    @PreAuthorize(Authz.DOCTOR)
    public ProblemDto updateProblem(@PathVariable Long id, @Valid @RequestBody ProblemRequest request) {
        return service.updateProblem(id, request);
    }

    // ----- immunizations -----

    @GetMapping("/api/patients/{patientId}/immunizations")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<ImmunizationDto> immunizations(@PathVariable Long patientId) {
        return service.immunizations(patientId);
    }

    @PostMapping("/api/patients/{patientId}/immunizations")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImmunizationDto addImmunization(@PathVariable Long patientId, @Valid @RequestBody ImmunizationRequest request) {
        return service.addImmunization(patientId, request);
    }

    @PutMapping("/api/immunizations/{id}")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    public ImmunizationDto updateImmunization(@PathVariable Long id, @Valid @RequestBody ImmunizationRequest request) {
        return service.updateImmunization(id, request);
    }

    // ----- family history -----

    @GetMapping("/api/patients/{patientId}/family-history")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<FamilyHistoryDto> familyHistory(@PathVariable Long patientId) {
        return service.familyHistory(patientId);
    }

    @PostMapping("/api/patients/{patientId}/family-history")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyHistoryDto addFamilyHistory(@PathVariable Long patientId, @Valid @RequestBody FamilyHistoryRequest request) {
        return service.addFamilyHistory(patientId, request);
    }

    @PutMapping("/api/family-history/{id}")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    public FamilyHistoryDto updateFamilyHistory(@PathVariable Long id, @Valid @RequestBody FamilyHistoryRequest request) {
        return service.updateFamilyHistory(id, request);
    }
}
