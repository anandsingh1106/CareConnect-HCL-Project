package com.careconnect.patient.web;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.patient.dto.PatientDtos.PatientRequest;
import com.careconnect.patient.dto.PatientDtos.PatientSummaryDto;
import com.careconnect.patient.service.PatientService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Patients")
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @Operation(summary = "Search patients by name, MRN, date of birth or phone (paged, sortable)")
    @GetMapping
    @PreAuthorize(Authz.PATIENT_SEARCH)
    public PageResponse<PatientSummaryDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mrn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dob,
            @RequestParam(required = false) String phone,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return patientService.search(q, mrn, dob, phone, pageable);
    }

    @Operation(summary = "Register a new patient (an MRN is generated)")
    @PostMapping
    @PreAuthorize(Authz.REGISTRATION)
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDto register(@Valid @RequestBody PatientRequest request) {
        return patientService.register(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Authz.DEMOGRAPHICS_READ)
    public PatientDto get(@PathVariable Long id) {
        return patientService.get(id);
    }

    @Operation(summary = "Update demographics (send the last seen 'version' to detect concurrent edits)")
    @PutMapping("/{id}")
    @PreAuthorize(Authz.REGISTRATION)
    public PatientDto update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request);
    }

    @Operation(summary = "Soft-delete a patient record (admin)")
    @DeleteMapping("/{id}")
    @PreAuthorize(Authz.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        patientService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
