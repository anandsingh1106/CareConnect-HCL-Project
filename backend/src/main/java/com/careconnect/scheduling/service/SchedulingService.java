package com.careconnect.scheduling.service;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.notification.service.NotificationService;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.scheduling.dto.SchedulingDtos.AppointmentDto;
import com.careconnect.scheduling.dto.SchedulingDtos.GenerateSlotsRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.GenerateSlotsResult;
import com.careconnect.scheduling.dto.SchedulingDtos.SlotDto;
import com.careconnect.scheduling.mapper.SchedulingMapper;
import com.careconnect.scheduling.model.Appointment;
import com.careconnect.scheduling.model.ProviderSlot;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentType;
import com.careconnect.scheduling.model.SchedulingEnums.BookedVia;
import com.careconnect.scheduling.model.SchedulingEnums.SlotStatus;
import com.careconnect.scheduling.repository.AppointmentRepository;
import com.careconnect.scheduling.repository.ProviderSlotRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Provider availability slots and appointment booking. */
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Set<AppointmentStatus> ACTIVE = EnumSet.of(AppointmentStatus.BOOKED, AppointmentStatus.CHECKED_IN);
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
            AppointmentStatus.BOOKED, EnumSet.of(AppointmentStatus.CHECKED_IN, AppointmentStatus.NO_SHOW, AppointmentStatus.COMPLETED),
            AppointmentStatus.CHECKED_IN, EnumSet.of(AppointmentStatus.COMPLETED));

    private final ProviderSlotRepository slots;
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final SchedulingMapper mapper;

    // ------------------------------------------------------------------ slots

    @Transactional(readOnly = true)
    public List<SlotDto> slots(Long providerId, LocalDate date, boolean availableOnly) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        List<ProviderSlot> list = availableOnly
                ? slots.findByProviderIdAndStatusAndSlotStartBetweenOrderBySlotStartAsc(providerId, SlotStatus.AVAILABLE, from, to)
                : slots.findByProviderIdAndSlotStartBetweenOrderBySlotStartAsc(providerId, from, to);
        LocalDateTime now = LocalDateTime.now();
        return list.stream()
                .filter(s -> !availableOnly || s.getSlotStart().isAfter(now))
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public GenerateSlotsResult generate(GenerateSlotsRequest request) {
        UserPrincipal current = SecurityUtils.currentUser();
        if (current.hasRole(Role.DOCTOR) && !current.id().equals(request.providerId())) {
            throw ApiException.forbidden("Doctors can only manage their own availability.");
        }
        AppUser provider = requireDoctor(request.providerId());
        LocalDate until = request.untilDate() == null ? request.date() : request.untilDate();
        if (until.isBefore(request.date()) || until.isAfter(request.date().plusDays(31))) {
            throw ApiException.badRequest("The end date must be within 31 days after the start date.");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw ApiException.badRequest("End time must be after start time.");
        }
        return createSlots(provider, request.date(), until, request.startTime(), request.endTime(), request.slotMinutes());
    }

    /** Shared by the API and the demo-data initializer. */
    @Transactional
    public GenerateSlotsResult createSlots(AppUser provider, LocalDate from, LocalDate until,
                                           java.time.LocalTime start, java.time.LocalTime end, int minutes) {
        int created = 0;
        int skipped = 0;
        for (LocalDate day = from; !day.isAfter(until); day = day.plusDays(1)) {
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            for (LocalDateTime t = day.atTime(start); !t.plusMinutes(minutes).isAfter(day.atTime(end)); t = t.plusMinutes(minutes)) {
                if (slots.existsByProviderIdAndSlotStart(provider.getId(), t)) {
                    skipped++;
                    continue;
                }
                ProviderSlot slot = new ProviderSlot();
                slot.setProvider(provider);
                slot.setSlotStart(t);
                slot.setSlotEnd(t.plusMinutes(minutes));
                slot.setStatus(SlotStatus.AVAILABLE);
                slots.save(slot);
                created++;
            }
        }
        return new GenerateSlotsResult(created, skipped);
    }

    @Transactional
    public SlotDto setBlocked(Long slotId, boolean blocked) {
        ProviderSlot slot = slots.findById(slotId).orElseThrow(() -> ApiException.notFound("Slot", slotId));
        UserPrincipal current = SecurityUtils.currentUser();
        if (current.hasRole(Role.DOCTOR) && !current.id().equals(slot.getProvider().getId())) {
            throw ApiException.forbidden("Doctors can only manage their own availability.");
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw ApiException.conflict("SLOT_BOOKED", "Cancel the appointment before blocking this slot.");
        }
        slot.setStatus(blocked ? SlotStatus.BLOCKED : SlotStatus.AVAILABLE);
        return mapper.toDto(slot);
    }

    // ------------------------------------------------------------------ appointments

    @Transactional
    public AppointmentDto book(Long patientId, Long slotId, AppointmentType type, String reason, BookedVia via) {
        Patient patient = patients.require(patientId);
        ProviderSlot slot = slots.findById(slotId).orElseThrow(() -> ApiException.notFound("Slot", slotId));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw ApiException.conflict("SLOT_TAKEN", "This slot is no longer available. Choose another time.");
        }
        if (!slot.getSlotStart().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Past time slots cannot be booked.");
        }
        if (appointments.patientHasOverlap(patientId, slot.getSlotStart(), slot.getSlotEnd(), ACTIVE)) {
            throw ApiException.conflict("PATIENT_BUSY", "The patient already has an appointment at this time.");
        }
        slot.setStatus(SlotStatus.BOOKED);   // @Version on the slot stops a double booking by two users

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setProvider(slot.getProvider());
        appointment.setSlot(slot);
        appointment.setStartTime(slot.getSlotStart());
        appointment.setEndTime(slot.getSlotEnd());
        appointment.setAppointmentType(type == null ? AppointmentType.IN_PERSON : type);
        appointment.setReason(reason);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setBookedVia(via);
        appointments.save(appointment);

        notifications.notifyUser(slot.getProvider().getId(), "New appointment",
                patient.getFullName() + " booked " + slot.getSlotStart().format(WHEN)
                        + (via == BookedVia.PORTAL ? " via the patient portal." : "."),
                NotificationService.TYPE_APPOINTMENT, "/appointments");
        return mapper.toDto(appointment);
    }

    /** Staff can cancel any booked appointment; patients only their own (enforced by the portal service). */
    @Transactional
    public AppointmentDto cancel(Long appointmentId, String reason, Long requiredPatientId) {
        Appointment appointment = find(appointmentId);
        if (requiredPatientId != null && !appointment.getPatient().getId().equals(requiredPatientId)) {
            throw ApiException.notFound("Appointment", appointmentId);
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw ApiException.conflict("INVALID_STATUS", "Only booked appointments can be cancelled.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason.trim());
        if (appointment.getSlot() != null && appointment.getSlot().getSlotStart().isAfter(LocalDateTime.now())) {
            appointment.getSlot().setStatus(SlotStatus.AVAILABLE);
        }
        if (requiredPatientId != null) {
            notifications.notifyUser(appointment.getProvider().getId(), "Appointment cancelled",
                    appointment.getPatient().getFullName() + " cancelled the appointment on "
                            + appointment.getStartTime().format(WHEN) + ": " + reason.trim(),
                    NotificationService.TYPE_APPOINTMENT, "/appointments");
        }
        return mapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDto updateStatus(Long appointmentId, AppointmentStatus next) {
        Appointment appointment = find(appointmentId);
        if (!ALLOWED.getOrDefault(appointment.getStatus(), Set.of()).contains(next)) {
            throw ApiException.conflict("INVALID_STATUS",
                    "An appointment that is " + appointment.getStatus() + " cannot become " + next + ".");
        }
        appointment.setStatus(next);
        return mapper.toDto(appointment);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentDto> search(Long providerId, Long patientId, AppointmentStatus status,
                                               LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.of(appointments.search(providerId, patientId, status,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), pageable), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> forPatient(Long patientId) {
        patients.require(patientId);
        return appointments.findByPatientIdOrderByStartTimeDesc(patientId).stream().map(mapper::toDto).toList();
    }

    private Appointment find(Long id) {
        Appointment appointment = appointments.findById(id).orElseThrow(() -> ApiException.notFound("Appointment", id));
        AuditContext.setPatientId(appointment.getPatient().getId());
        return appointment;
    }

    private AppUser requireDoctor(Long id) {
        AppUser user = users.require(id);
        if (user.getRole() != Role.DOCTOR || !user.isActive()) {
            throw ApiException.badRequest("Slots can only be created for active doctors.");
        }
        return user;
    }
}
