package com.careconnect.clinical.web;

import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.clinical.dto.ClinicalDtos.VitalRequest;
import com.careconnect.clinical.service.VitalService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Vitals")
@RestController
@RequiredArgsConstructor
public class VitalController {

    private final VitalService vitalService;

    @Operation(summary = "Record vital signs (BMI is calculated automatically)")
    @PostMapping("/api/patients/{patientId}/vitals")
    @PreAuthorize(Authz.DOCTOR_OR_NURSE)
    @ResponseStatus(HttpStatus.CREATED)
    public VitalDto record(@PathVariable Long patientId, @Valid @RequestBody VitalRequest request) {
        return vitalService.record(patientId, request);
    }

    @Operation(summary = "Vital sign history, oldest first (for trend charts)")
    @GetMapping("/api/patients/{patientId}/vitals")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<VitalDto> history(@PathVariable Long patientId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return vitalService.history(patientId, from, to);
    }
}
