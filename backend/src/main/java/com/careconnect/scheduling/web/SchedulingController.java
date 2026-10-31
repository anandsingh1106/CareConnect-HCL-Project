package com.careconnect.scheduling.web;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.scheduling.dto.SchedulingDtos.AppointmentDto;
import com.careconnect.scheduling.dto.SchedulingDtos.AppointmentStatusRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.BookAppointmentRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.CancelAppointmentRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.GenerateSlotsRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.GenerateSlotsResult;
import com.careconnect.scheduling.dto.SchedulingDtos.SlotDto;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import com.careconnect.scheduling.model.SchedulingEnums.BookedVia;
import com.careconnect.scheduling.service.SchedulingService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Scheduling")
@RestController
@RequiredArgsConstructor
public class SchedulingController {

    private final SchedulingService schedulingService;

    @Operation(summary = "A doctor's slots on a date (availableOnly=true for booking)")
    @GetMapping("/api/slots")
    @PreAuthorize(Authz.SCHEDULING)
    public List<SlotDto> slots(@RequestParam Long providerId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(defaultValue = "false") boolean availableOnly) {
        return schedulingService.slots(providerId, date, availableOnly);
    }

    @Operation(summary = "Generate availability slots for a doctor over a date range")
    @PostMapping("/api/slots/generate")
    @PreAuthorize(Authz.SCHEDULING)
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateSlotsResult generate(@Valid @RequestBody GenerateSlotsRequest request) {
        return schedulingService.generate(request);
    }

    @PostMapping("/api/slots/{id}/block")
    @PreAuthorize(Authz.SCHEDULING)
    public SlotDto block(@PathVariable Long id) {
        return schedulingService.setBlocked(id, true);
    }

    @PostMapping("/api/slots/{id}/unblock")
    @PreAuthorize(Authz.SCHEDULING)
    public SlotDto unblock(@PathVariable Long id) {
        return schedulingService.setBlocked(id, false);
    }

    @GetMapping("/api/appointments")
    @PreAuthorize(Authz.SCHEDULING)
    public PageResponse<AppointmentDto> search(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return schedulingService.search(providerId, patientId, status, from, to, pageable);
    }

    @GetMapping("/api/patients/{patientId}/appointments")
    @PreAuthorize(Authz.SCHEDULING)
    public List<AppointmentDto> forPatient(@PathVariable Long patientId) {
        return schedulingService.forPatient(patientId);
    }

    @Operation(summary = "Book an appointment into a free slot")
    @PostMapping("/api/appointments")
    @PreAuthorize(Authz.SCHEDULING)
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto book(@Valid @RequestBody BookAppointmentRequest request) {
        return schedulingService.book(request.patientId(), request.slotId(), request.appointmentType(),
                request.reason(), BookedVia.STAFF);
    }

    @PostMapping("/api/appointments/{id}/cancel")
    @PreAuthorize(Authz.SCHEDULING)
    public AppointmentDto cancel(@PathVariable Long id, @Valid @RequestBody CancelAppointmentRequest request) {
        return schedulingService.cancel(id, request.reason(), null);
    }

    @Operation(summary = "Check in, complete or mark no-show")
    @PatchMapping("/api/appointments/{id}/status")
    @PreAuthorize(Authz.SCHEDULING)
    public AppointmentDto updateStatus(@PathVariable Long id, @Valid @RequestBody AppointmentStatusRequest request) {
        return schedulingService.updateStatus(id, request.status());
    }
}
