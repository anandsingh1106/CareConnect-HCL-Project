package com.careconnect.portal.web;

import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.order.dto.OrderDtos.LabResultDto;
import com.careconnect.patient.dto.PatientDtos.AllergyDto;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.patient.dto.PatientDtos.ProblemDto;
import com.careconnect.portal.dto.PortalDtos.VisitSummaryDto;
import com.careconnect.portal.service.PortalService;
import com.careconnect.scheduling.dto.SchedulingDtos.AppointmentDto;
import com.careconnect.scheduling.dto.SchedulingDtos.CancelAppointmentRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.PortalBookRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.SlotDto;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Patient portal. Only PATIENT users, and only their own data. */
@Tag(name = "Patient portal")
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@PreAuthorize(Authz.PATIENT)
public class PortalController {

    private final PortalService portalService;

    @GetMapping("/profile")
    public PatientDto profile() {
        return portalService.profile();
    }

    @GetMapping("/allergies")
    public List<AllergyDto> allergies() {
        return portalService.allergies();
    }

    @GetMapping("/problems")
    public List<ProblemDto> problems() {
        return portalService.problems();
    }

    @GetMapping("/medications")
    public List<PrescriptionDto> medications() {
        return portalService.medications();
    }

    @GetMapping("/lab-results")
    public List<LabResultDto> labResults() {
        return portalService.labResults();
    }

    @GetMapping("/vitals")
    public List<VitalDto> vitals() {
        return portalService.vitals();
    }

    @Operation(summary = "Visit summaries (signed notes only)")
    @GetMapping("/visits")
    public List<VisitSummaryDto> visits() {
        return portalService.visits();
    }

    @Operation(summary = "Download a visit summary as PDF")
    @GetMapping(value = "/visits/{encounterId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> visitPdf(@PathVariable Long encounterId) {
        return pdf(portalService.visitPdf(encounterId), "visit-summary-" + encounterId + ".pdf");
    }

    @Operation(summary = "Download a prescription as PDF")
    @GetMapping(value = "/prescriptions/{prescriptionId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> prescriptionPdf(@PathVariable Long prescriptionId) {
        return pdf(portalService.prescriptionPdf(prescriptionId), "prescription-" + prescriptionId + ".pdf");
    }

    @GetMapping("/appointments")
    public List<AppointmentDto> appointments() {
        return portalService.appointments();
    }

    @GetMapping("/slots")
    public List<SlotDto> slots(@RequestParam Long providerId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return portalService.availableSlots(providerId, date);
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto book(@Valid @RequestBody PortalBookRequest request) {
        return portalService.book(request);
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentDto cancel(@PathVariable Long id, @Valid @RequestBody CancelAppointmentRequest request) {
        return portalService.cancel(id, request.reason());
    }

    private static ResponseEntity<byte[]> pdf(byte[] content, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(content);
    }
}
