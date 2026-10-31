package com.careconnect.config;

import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.scheduling.model.Appointment;
import com.careconnect.scheduling.model.ProviderSlot;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentType;
import com.careconnect.scheduling.model.SchedulingEnums.BookedVia;
import com.careconnect.scheduling.model.SchedulingEnums.SlotStatus;
import com.careconnect.scheduling.repository.AppointmentRepository;
import com.careconnect.scheduling.repository.ProviderSlotRepository;
import com.careconnect.scheduling.service.SchedulingService;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Development only (careconnect.demo-data.enabled=true): keeps the demo realistic by creating
 * appointment slots for the coming week and a few bookings relative to today's date.
 * The static demo data (patients, history, orders...) comes from Liquibase script 003-demo-data.sql.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "careconnect.demo-data", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final AppUserRepository users;
    private final PatientRepository patients;
    private final ProviderSlotRepository slots;
    private final AppointmentRepository appointments;
    private final SchedulingService schedulingService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (slots.existsBySlotStartAfter(LocalDateTime.now().plusDays(1))) {
            return;   // already prepared for this week
        }
        List<AppUser> doctors = users.findByRoleAndActiveTrueOrderByFullNameAsc(Role.DOCTOR);
        LocalDate today = LocalDate.now();
        for (AppUser doctor : doctors) {
            schedulingService.createSlots(doctor, today, today.plusDays(7), LocalTime.of(9, 0), LocalTime.of(13, 0), 30);
            schedulingService.createSlots(doctor, today, today.plusDays(7), LocalTime.of(14, 0), LocalTime.of(17, 0), 30);
        }
        LocalDate nextWorkday = today.plusDays(1).getDayOfWeek() == DayOfWeek.SUNDAY ? today.plusDays(2) : today.plusDays(1);
        LocalDate dayAfter = nextWorkday.plusDays(1).getDayOfWeek() == DayOfWeek.SUNDAY ? nextWorkday.plusDays(2) : nextWorkday.plusDays(1);

        book("dr.mehta", "CC-2026-000001", nextWorkday.atTime(10, 0), "Diabetes review with HbA1c report", AppointmentType.IN_PERSON);
        book("dr.rao", "CC-2026-000003", dayAfter.atTime(11, 0), "INR follow-up", AppointmentType.IN_PERSON);
        book("dr.mehta", "CC-2026-000002", dayAfter.atTime(9, 30), "Asthma review", AppointmentType.TELEHEALTH);
        if (today.getDayOfWeek() != DayOfWeek.SUNDAY) {
            book("dr.mehta", "CC-2026-000004", today.atTime(16, 0), "Thyroid follow-up", AppointmentType.IN_PERSON);
            book("dr.mehta", "CC-2026-000005", today.atTime(16, 30), "Fever for two days", AppointmentType.IN_PERSON);
        }
        log.info("Demo data: created appointment slots for {} doctors and sample bookings", doctors.size());
    }

    private void book(String doctorUsername, String mrn, LocalDateTime start, String reason, AppointmentType type) {
        Optional<AppUser> doctor = users.findByUsernameIgnoreCase(doctorUsername);
        Optional<Patient> patient = patients.search(null, mrn, null, null,
                org.springframework.data.domain.Pageable.ofSize(1)).stream().findFirst();
        if (doctor.isEmpty() || patient.isEmpty()) {
            return;
        }
        slots.findByProviderIdAndSlotStartBetweenOrderBySlotStartAsc(doctor.get().getId(), start, start).stream()
                .filter(s -> s.getStatus() == SlotStatus.AVAILABLE)
                .findFirst()
                .ifPresent(slot -> createAppointment(slot, patient.get(), reason, type));
    }

    private void createAppointment(ProviderSlot slot, Patient patient, String reason, AppointmentType type) {
        slot.setStatus(SlotStatus.BOOKED);
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setProvider(slot.getProvider());
        appointment.setSlot(slot);
        appointment.setStartTime(slot.getSlotStart());
        appointment.setEndTime(slot.getSlotEnd());
        appointment.setAppointmentType(type);
        appointment.setReason(reason);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setBookedVia(BookedVia.STAFF);
        appointments.save(appointment);
    }
}
