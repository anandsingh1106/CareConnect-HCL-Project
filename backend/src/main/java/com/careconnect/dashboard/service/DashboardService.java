package com.careconnect.dashboard.service;

import com.careconnect.audit.repository.AuditLogRepository;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.repository.ClinicalNoteRepository;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.clinical.repository.VitalRepository;
import com.careconnect.dashboard.dto.DashboardDtos.ChartData;
import com.careconnect.dashboard.dto.DashboardDtos.DashboardDto;
import com.careconnect.dashboard.dto.DashboardDtos.Series;
import com.careconnect.dashboard.dto.DashboardDtos.StatCard;
import com.careconnect.dashboard.dto.DashboardDtos.WorkItem;
import com.careconnect.dashboard.dto.DashboardDtos.WorkList;
import com.careconnect.medication.model.MedicationEnums.AdministrationStatus;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.DrugRepository;
import com.careconnect.medication.repository.MedicationAdministrationRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.messaging.repository.MessageRepository;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.repository.ClinicalOrderRepository;
import com.careconnect.order.repository.LabResultRepository;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.scheduling.model.Appointment;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import com.careconnect.scheduling.model.SchedulingEnums.SlotStatus;
import com.careconnect.scheduling.repository.AppointmentRepository;
import com.careconnect.scheduling.repository.ProviderSlotRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;

/** Role-based dashboards: every role sees the numbers that drive its own daily work. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yy");

    private final PatientRepository patients;
    private final AppUserRepository users;
    private final EncounterRepository encounters;
    private final ClinicalNoteRepository notes;
    private final VitalRepository vitals;
    private final ClinicalOrderRepository orders;
    private final LabResultRepository results;
    private final PrescriptionRepository prescriptions;
    private final MedicationAdministrationRepository administrations;
    private final DrugRepository drugs;
    private final AppointmentRepository appointments;
    private final ProviderSlotRepository slots;
    private final MessageRepository messages;
    private final AuditLogRepository auditLogs;

    @Transactional(readOnly = true)
    public DashboardDto forCurrentUser() {
        UserPrincipal user = SecurityUtils.currentUser();
        return switch (user.role()) {
            case ADMIN -> admin();
            case DOCTOR -> doctor(user);
            case NURSE -> nurse();
            case PHARMACIST -> pharmacist();
            case RECEPTIONIST -> receptionist();
            case PATIENT -> patient(user);
        };
    }

    // ------------------------------------------------------------------ roles

    private DashboardDto admin() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<StatCard> cards = List.of(
                new StatCard("patients", "Registered patients", patients.countByDeletedFalse(), "groups", "primary", "/patients"),
                new StatCard("users", "Active user accounts", users.countByActiveTrue(), "manage_accounts", "info", "/admin/users"),
                new StatCard("encounters", "Visits this month", encounters.countByEncounterDateBetween(
                        LocalDate.now().withDayOfMonth(1).atStartOfDay(), LocalDateTime.now()), "stethoscope", "success", null),
                new StatCard("lowStock", "Drugs below reorder level", drugs.countLowStock(), "inventory_2", "warning", "/pharmacy/inventory"),
                new StatCard("audit", "Audit events today", auditLogs.countByCreatedAtBetween(today, today.plusDays(1)), "policy", "danger", "/admin/audit"));

        List<String> roleLabels = Arrays.stream(Role.values()).map(r -> r.name().charAt(0) + r.name().substring(1).toLowerCase()).toList();
        List<Long> roleCounts = Arrays.stream(Role.values()).map(users::countByRoleAndActiveTrue).toList();

        List<ChartData> charts = List.of(
                new ChartData("usersByRole", "Users by role", "doughnut", roleLabels, List.of(new Series("Users", roleCounts))),
                lastDays("auditTrend", "Audit events - last 7 days", "line", "Events", auditLogs::countByCreatedAtBetween),
                lastMonths("registrations", "New patients - last 6 months", "Patients", patients::countByDeletedFalseAndCreatedAtBetween));
        return new DashboardDto("ADMIN", cards, charts, null);
    }

    private DashboardDto doctor(UserPrincipal user) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<Appointment> todays = appointments.findByProviderIdAndStartTimeBetweenOrderByStartTimeAsc(user.id(), today, today.plusDays(1))
                .stream().filter(a -> a.getStatus() != AppointmentStatus.CANCELLED).toList();
        EnumSet<OrderStatus> pending = EnumSet.of(OrderStatus.PLACED, OrderStatus.IN_PROGRESS);

        List<StatCard> cards = List.of(
                new StatCard("appointmentsToday", "Appointments today", todays.size(), "event", "primary", "/appointments"),
                new StatCard("openEncounters", "Open encounters", encounters.countByProviderIdAndStatus(user.id(), EncounterStatus.OPEN), "assignment", "info", "/encounters"),
                new StatCard("unsignedNotes", "Unsigned notes", notes.countByAuthorIdAndStatus(user.id(), NoteStatus.DRAFT), "edit_note", "warning", "/encounters"),
                new StatCard("pendingOrders", "My pending orders", orders.countByOrderedByIdAndStatusIn(user.id(), pending), "science", "success", "/orders"),
                new StatCard("unreadMessages", "Unread messages", messages.countByRecipientIdAndReadAtIsNull(user.id()), "mail", "danger", "/messages"));

        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusLabels.add(s.name().replace('_', ' '));
            statusCounts.add(orders.countByOrderedByIdAndStatus(user.id(), s));
        }
        List<ChartData> charts = List.of(
                lastDays("visits", "My visits - last 7 days", "bar", "Visits",
                        (from, to) -> encounters.countByProviderIdAndEncounterDateBetween(user.id(), from, to)),
                new ChartData("ordersByStatus", "My orders by status", "doughnut", statusLabels, List.of(new Series("Orders", statusCounts))));

        List<WorkItem> items = todays.stream().map(a -> new WorkItem(
                a.getPatient().getFullName() + " (" + a.getPatient().getMrn() + ")",
                a.getReason() == null ? a.getAppointmentType().name().replace('_', ' ') : a.getReason(),
                a.getStartTime().format(TIME) + " - " + a.getStatus().name().replace('_', ' '),
                "/patients/" + a.getPatient().getId())).toList();
        return new DashboardDto("DOCTOR", cards, charts, new WorkList("Today's appointments", items));
    }

    private DashboardDto nurse() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        List<StatCard> cards = List.of(
                new StatCard("vitalsToday", "Vitals recorded today", vitals.countByRecordedAtBetween(today, tomorrow), "monitor_heart", "primary", "/nursing"),
                new StatCard("marToday", "Doses documented today", administrations.countByAdministeredAtBetween(today, tomorrow), "medication", "success", "/nursing"),
                new StatCard("activeMeds", "Verified medicines on MAR", prescriptions.countByStatus(PrescriptionStatus.VERIFIED)
                        + prescriptions.countByStatus(PrescriptionStatus.DISPENSED), "vaccines", "info", "/nursing"),
                new StatCard("pendingTests", "Pending lab / imaging orders", orders.countByStatusInAndOrderTypeIn(
                        EnumSet.of(OrderStatus.PLACED, OrderStatus.IN_PROGRESS), EnumSet.of(OrderType.LAB, OrderType.IMAGING)), "biotech", "warning", "/orders"));

        List<String> labels = new ArrayList<>();
        List<Long> given = new ArrayList<>();
        List<Long> notGiven = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime from = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime to = from.plusDays(1);
            labels.add(from.format(DAY));
            long g = administrations.countByStatusAndAdministeredAtBetween(AdministrationStatus.GIVEN, from, to);
            given.add(g);
            notGiven.add(administrations.countByAdministeredAtBetween(from, to) - g);
        }
        List<ChartData> charts = List.of(
                new ChartData("mar", "Medication administration - last 7 days", "bar", labels,
                        List.of(new Series("Given", given), new Series("Held / refused / missed", notGiven))),
                lastDays("vitals", "Vitals recorded - last 7 days", "line", "Vital sets", vitals::countByRecordedAtBetween));
        return new DashboardDto("NURSE", cards, charts, null);
    }

    private DashboardDto pharmacist() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<StatCard> cards = List.of(
                new StatCard("pendingVerification", "Awaiting verification", prescriptions.countByStatus(PrescriptionStatus.PENDING_VERIFICATION), "fact_check", "warning", "/pharmacy"),
                new StatCard("readyToDispense", "Ready to dispense", prescriptions.countByStatus(PrescriptionStatus.VERIFIED), "local_pharmacy", "primary", "/pharmacy"),
                new StatCard("dispensedToday", "Dispensed today", prescriptions.countByStatusAndDispensedAtBetween(
                        PrescriptionStatus.DISPENSED, today, today.plusDays(1)), "done_all", "success", "/pharmacy"),
                new StatCard("lowStock", "Low-stock drugs", drugs.countLowStock(), "inventory_2", "danger", "/pharmacy/inventory"));

        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        for (PrescriptionStatus s : PrescriptionStatus.values()) {
            statusLabels.add(s.name().replace('_', ' '));
            statusCounts.add(prescriptions.countByStatus(s));
        }
        List<ChartData> charts = List.of(
                lastDays("dispensed", "Prescriptions dispensed - last 7 days", "bar", "Dispensed",
                        (from, to) -> prescriptions.countByStatusAndDispensedAtBetween(PrescriptionStatus.DISPENSED, from, to)),
                new ChartData("rxByStatus", "Prescriptions by status", "doughnut", statusLabels, List.of(new Series("Prescriptions", statusCounts))));

        List<WorkItem> lowStock = drugs.findLowStock().stream().limit(8).map(d -> new WorkItem(d.getName(),
                d.getDrugClass(), d.getStockQty() + " left (reorder at " + d.getReorderLevel() + ")", "/pharmacy/inventory")).toList();
        return new DashboardDto("PHARMACIST", cards, charts, new WorkList("Low stock", lowStock));
    }

    private DashboardDto receptionist() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        List<StatCard> cards = List.of(
                new StatCard("appointmentsToday", "Appointments today", appointments.countByStartTimeBetween(today, tomorrow)
                        - appointments.countByStatusAndStartTimeBetween(AppointmentStatus.CANCELLED, today, tomorrow), "event", "primary", "/appointments"),
                new StatCard("checkedIn", "Checked in today", appointments.countByStatusAndStartTimeBetween(AppointmentStatus.CHECKED_IN, today, tomorrow), "how_to_reg", "success", "/appointments"),
                new StatCard("registrations", "New patients this week", patients.countByDeletedFalseAndCreatedAtBetween(
                        LocalDate.now().minusDays(6).atStartOfDay(), tomorrow), "person_add", "info", "/patients"),
                new StatCard("freeSlots", "Free slots today", slots.countByStatusAndSlotStartBetween(SlotStatus.AVAILABLE, LocalDateTime.now(), tomorrow), "event_available", "warning", "/appointments"));

        List<String> labels = new ArrayList<>();
        List<Long> booked = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDateTime from = LocalDate.now().plusDays(i).atStartOfDay();
            labels.add(from.format(DAY));
            booked.add(appointments.countByStartTimeBetween(from, from.plusDays(1))
                    - appointments.countByStatusAndStartTimeBetween(AppointmentStatus.CANCELLED, from, from.plusDays(1)));
        }
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        for (AppointmentStatus s : AppointmentStatus.values()) {
            statusLabels.add(s.name().replace('_', ' '));
            statusCounts.add(appointments.countByStatusAndStartTimeBetween(s, today, tomorrow));
        }
        List<ChartData> charts = List.of(
                new ChartData("upcoming", "Appointments - next 7 days", "bar", labels, List.of(new Series("Appointments", booked))),
                new ChartData("todayByStatus", "Today by status", "doughnut", statusLabels, List.of(new Series("Appointments", statusCounts))));

        List<WorkItem> items = appointments.search(null, null, null, today, tomorrow,
                        PageRequest.of(0, 10, Sort.by("startTime"))).getContent().stream()
                .map(a -> new WorkItem(a.getPatient().getFullName(), "with " + a.getProvider().getFullName(),
                        a.getStartTime().format(TIME) + " - " + a.getStatus().name().replace('_', ' '), "/appointments"))
                .toList();
        return new DashboardDto("RECEPTIONIST", cards, charts, new WorkList("Today's schedule", items));
    }

    private DashboardDto patient(UserPrincipal user) {
        Long patientId = user.patientId();
        long upcoming = patientId == null ? 0 : appointments.findByPatientIdAndStartTimeAfterAndStatusInOrderByStartTimeAsc(
                patientId, LocalDateTime.now(), EnumSet.of(AppointmentStatus.BOOKED)).size();
        long activeMeds = patientId == null ? 0 : prescriptions.findByPatientIdAndStatusIn(patientId, PrescriptionStatus.OPEN)
                .stream().filter(Prescription::isCurrentlyActive).count();
        long resultsCount = patientId == null ? 0 : results.findForPatient(patientId).size();
        List<StatCard> cards = List.of(
                new StatCard("upcoming", "Upcoming appointments", upcoming, "event", "primary", "/portal/appointments"),
                new StatCard("medications", "Current medicines", activeMeds, "medication", "success", "/portal/health"),
                new StatCard("results", "Lab results", resultsCount, "science", "info", "/portal/health"),
                new StatCard("messages", "Unread messages", messages.countByRecipientIdAndReadAtIsNull(user.id()), "mail", "warning", "/portal/messages"));
        return new DashboardDto("PATIENT", cards, List.of(), null);
    }

    // ------------------------------------------------------------------ chart helpers

    private ChartData lastDays(String key, String title, String type, String seriesName,
                               BiFunction<LocalDateTime, LocalDateTime, Long> counter) {
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime from = LocalDate.now().minusDays(i).atStartOfDay();
            labels.add(from.format(DAY));
            data.add(counter.apply(from, from.plusDays(1)));
        }
        return new ChartData(key, title, type, labels, List.of(new Series(seriesName, data)));
    }

    private ChartData lastMonths(String key, String title, String seriesName,
                                 BiFunction<LocalDateTime, LocalDateTime, Long> counter) {
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            labels.add(month.format(MONTH));
            data.add(counter.apply(month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()));
        }
        return new ChartData(key, title, "bar", labels, List.of(new Series(seriesName, data)));
    }
}
