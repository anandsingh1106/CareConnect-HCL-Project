package com.careconnect.scheduling.dto;

import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentType;
import com.careconnect.scheduling.model.SchedulingEnums.BookedVia;
import com.careconnect.scheduling.model.SchedulingEnums.SlotStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class SchedulingDtos {

    private SchedulingDtos() {
    }

    public record SlotDto(Long id, Long providerId, String providerName, LocalDateTime slotStart,
                          LocalDateTime slotEnd, SlotStatus status) {
    }

    /** Creates equal slots between startTime and endTime on every day from date to untilDate (Sundays skipped). */
    public record GenerateSlotsRequest(
            @NotNull Long providerId,
            @NotNull @FutureOrPresent LocalDate date,
            LocalDate untilDate,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @NotNull @Min(10) @Max(120) Integer slotMinutes) {
    }

    public record GenerateSlotsResult(int created, int skipped) {
    }

    public record AppointmentDto(
            Long id,
            Long patientId,
            String patientName,
            String patientMrn,
            Long providerId,
            String providerName,
            String providerSpecialty,
            Long slotId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AppointmentType appointmentType,
            String reason,
            AppointmentStatus status,
            BookedVia bookedVia,
            String cancelReason,
            LocalDateTime createdAt) {
    }

    public record BookAppointmentRequest(
            @NotNull Long patientId,
            @NotNull Long slotId,
            AppointmentType appointmentType,
            @Size(max = 500) String reason) {
    }

    public record PortalBookRequest(
            @NotNull Long slotId,
            AppointmentType appointmentType,
            @Size(max = 500) String reason) {
    }

    public record CancelAppointmentRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {
    }
}
