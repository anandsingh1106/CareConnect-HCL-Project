package com.careconnect.clinical.web;

import com.careconnect.clinical.dto.ClinicalDtos.DiagnosisDto;
import com.careconnect.clinical.dto.ClinicalDtos.DiagnosisRequest;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterDetailDto;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterDto;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterRequest;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.service.EncounterService;
import com.careconnect.common.dto.PageResponse;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Encounters")
@RestController
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;

    @Operation(summary = "Open an encounter (visit) for a patient")
    @PostMapping("/api/encounters")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public EncounterDto open(@Valid @RequestBody EncounterRequest request) {
        return encounterService.open(request);
    }

    @Operation(summary = "Worklist: my encounters (doctor) or all encounters (nurse) with the given status")
    @GetMapping("/api/encounters")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    public List<EncounterDto> worklist(@RequestParam(required = false) EncounterStatus status) {
        return encounterService.worklist(status);
    }

    @Operation(summary = "Encounter with notes, diagnoses and vitals")
    @GetMapping("/api/encounters/{id}")
    @PreAuthorize(Authz.CLINICAL_READ)
    public EncounterDetailDto detail(@PathVariable Long id) {
        return encounterService.detail(id);
    }

    @GetMapping("/api/patients/{patientId}/encounters")
    @PreAuthorize(Authz.CLINICAL_READ)
    public PageResponse<EncounterDto> forPatient(@PathVariable Long patientId,
                                                 @PageableDefault(size = 10) Pageable pageable) {
        return encounterService.forPatient(patientId, pageable);
    }

    @Operation(summary = "Close an encounter (all notes must be signed)")
    @PostMapping("/api/encounters/{id}/close")
    @PreAuthorize(Authz.DOCTOR)
    public EncounterDto close(@PathVariable Long id) {
        return encounterService.close(id);
    }

    @Operation(summary = "Add an ICD-10 diagnosis (PRIMARY replaces the current primary)")
    @PostMapping("/api/encounters/{id}/diagnoses")
    @PreAuthorize(Authz.DOCTOR)
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisDto addDiagnosis(@PathVariable Long id, @Valid @RequestBody DiagnosisRequest request) {
        return encounterService.addDiagnosis(id, request);
    }

    @DeleteMapping("/api/diagnoses/{id}")
    @PreAuthorize(Authz.DOCTOR)
    public ResponseEntity<Void> removeDiagnosis(@PathVariable Long id) {
        encounterService.removeDiagnosis(id);
        return ResponseEntity.noContent().build();
    }
}
