package com.careconnect.medication.web;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.medication.dto.MedicationDtos.DispenseRequest;
import com.careconnect.medication.dto.MedicationDtos.ModifyPrescriptionRequest;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDetailDto;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.dto.MedicationDtos.ReasonRequest;
import com.careconnect.medication.dto.MedicationDtos.VerifyRequest;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.service.PrescriptionService;
import com.careconnect.order.dto.OrderDtos.OrderDetailDto;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Medication - Prescriptions and pharmacy")
@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "Prescription queue, e.g. status=PENDING_VERIFICATION for the pharmacist")
    @GetMapping("/api/prescriptions")
    @PreAuthorize(Authz.CLINICAL_READ)
    public PageResponse<PrescriptionDto> search(@RequestParam(required = false) PrescriptionStatus status,
                                                @RequestParam(required = false) Long patientId,
                                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return prescriptionService.search(status, patientId, pageable);
    }

    @Operation(summary = "Prescription with its decision-support alerts and the patient's allergies")
    @GetMapping("/api/prescriptions/{id}")
    @PreAuthorize(Authz.CLINICAL_READ)
    public PrescriptionDetailDto detail(@PathVariable Long id) {
        return prescriptionService.detail(id);
    }

    @Operation(summary = "Medication list of a patient (activeOnly=true for current medicines)")
    @GetMapping("/api/patients/{patientId}/medications")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<PrescriptionDto> forPatient(@PathVariable Long patientId,
                                            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return prescriptionService.forPatient(patientId, activeOnly);
    }

    @PostMapping("/api/prescriptions/{id}/verify")
    @PreAuthorize(Authz.PHARMACIST)
    public PrescriptionDto verify(@PathVariable Long id, @Valid @RequestBody(required = false) VerifyRequest request) {
        return prescriptionService.verify(id, request == null ? null : request.note());
    }

    @PostMapping("/api/prescriptions/{id}/reject")
    @PreAuthorize(Authz.PHARMACIST)
    public PrescriptionDto reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return prescriptionService.reject(id, request.reason());
    }

    @Operation(summary = "Dispense a verified prescription (reduces stock)")
    @PostMapping("/api/prescriptions/{id}/dispense")
    @PreAuthorize(Authz.PHARMACIST)
    public PrescriptionDto dispense(@PathVariable Long id, @Valid @RequestBody(required = false) DispenseRequest request) {
        return prescriptionService.dispense(id, request == null ? null : request.quantity());
    }

    @PostMapping("/api/prescriptions/{id}/discontinue")
    @PreAuthorize(Authz.DOCTOR)
    public PrescriptionDto discontinue(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return prescriptionService.discontinue(id, request.reason());
    }

    @Operation(summary = "Change dose/frequency: discontinues this prescription and places a new medication order")
    @PostMapping("/api/prescriptions/{id}/modify")
    @PreAuthorize(Authz.DOCTOR)
    public OrderDetailDto modify(@PathVariable Long id, @Valid @RequestBody ModifyPrescriptionRequest request) {
        return prescriptionService.modify(id, request);
    }
}
