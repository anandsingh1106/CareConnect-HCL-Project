package com.careconnect.medication.web;

import com.careconnect.medication.dto.MedicationDtos.AdministrationDto;
import com.careconnect.medication.dto.MedicationDtos.AdministrationRequest;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.service.MarService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Medication Administration Record (MAR)")
@RestController
@RequiredArgsConstructor
public class MarController {

    private final MarService marService;

    @Operation(summary = "Active, verified medicines that can be administered to the patient")
    @GetMapping("/api/patients/{patientId}/mar/due")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    public List<PrescriptionDto> due(@PathVariable Long patientId) {
        return marService.administrable(patientId);
    }

    @GetMapping("/api/patients/{patientId}/mar")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<AdministrationDto> history(@PathVariable Long patientId) {
        return marService.history(patientId);
    }

    @Operation(summary = "Document a dose given / held / refused / missed")
    @PostMapping("/api/mar")
    @PreAuthorize(Authz.NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdministrationDto record(@Valid @RequestBody AdministrationRequest request) {
        return marService.record(request);
    }
}
