package com.careconnect.portal.service;

import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.ClinicalEnums.DiagnosisType;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.ClinicalNote;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.clinical.repository.ClinicalNoteRepository;
import com.careconnect.clinical.repository.EncounterDiagnosisRepository;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.clinical.repository.VitalRepository;
import com.careconnect.clinical.service.VitalService;
import com.careconnect.common.exception.ApiException;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.order.dto.OrderDtos.LabResultDto;
import com.careconnect.order.model.OrderEnums.ResultStatus;
import com.careconnect.order.service.OrderService;
import com.careconnect.patient.dto.PatientDtos.AllergyDto;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.patient.dto.PatientDtos.ProblemDto;
import com.careconnect.patient.mapper.PatientMapper;
import com.careconnect.patient.model.PatientEnums.ProblemStatus;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.patient.service.PatientHistoryService;
import com.careconnect.portal.dto.PortalDtos.VisitSummaryDto;
import com.careconnect.scheduling.dto.SchedulingDtos.AppointmentDto;
import com.careconnect.scheduling.dto.SchedulingDtos.PortalBookRequest;
import com.careconnect.scheduling.dto.SchedulingDtos.SlotDto;
import com.careconnect.scheduling.model.SchedulingEnums.BookedVia;
import com.careconnect.scheduling.service.SchedulingService;
import com.careconnect.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Patient self-service. Every method works on the logged-in patient's own record only
 * (the patient id comes from the JWT, never from the request), which enforces minimum-necessary access.
 */
@Service
@RequiredArgsConstructor
public class PortalService {

    private final PatientRepository patients;
    private final PatientHistoryService historyService;
    private final EncounterRepository encounters;
    private final EncounterDiagnosisRepository diagnoses;
    private final ClinicalNoteRepository notes;
    private final VitalRepository vitals;
    private final VitalService vitalService;
    private final PrescriptionRepository prescriptions;
    private final OrderService orderService;
    private final SchedulingService schedulingService;
    private final PatientMapper patientMapper;
    private final ClinicalMapper clinicalMapper;
    private final MedicationMapper medicationMapper;
    private final PdfService pdfService;

    @Transactional(readOnly = true)
    public PatientDto profile() {
        return patientMapper.toDto(patients.require(me()));
    }

    @Transactional(readOnly = true)
    public List<AllergyDto> allergies() {
        return historyService.allergies(me());
    }

    @Transactional(readOnly = true)
    public List<ProblemDto> problems() {
        return historyService.problems(me()).stream().filter(p -> p.status() == ProblemStatus.ACTIVE).toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> medications() {
        return prescriptions.findByPatientIdOrderByCreatedAtDesc(me()).stream()
                .filter(p -> p.getStatus() != PrescriptionStatus.DRAFT)
                .map(medicationMapper::toDto).toList();
    }

    /** Only final (or corrected) results are released to the patient. */
    @Transactional(readOnly = true)
    public List<LabResultDto> labResults() {
        return orderService.resultsForPatient(me()).stream()
                .filter(r -> r.resultStatus() != ResultStatus.PRELIMINARY).toList();
    }

    @Transactional(readOnly = true)
    public List<VitalDto> vitals() {
        return vitalService.history(me(), null, null);
    }

    @Transactional(readOnly = true)
    public List<VisitSummaryDto> visits() {
        Long patientId = me();
        patients.require(patientId);
        return encounters.findByPatientIdOrderByEncounterDateDesc(patientId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(e -> e.getStatus() != EncounterStatus.CANCELLED)
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] visitPdf(Long encounterId) {
        Encounter encounter = ownEncounter(encounterId);
        return pdfService.visitSummary(patientMapper.toDto(encounter.getPatient()), summary(encounter));
    }

    @Transactional(readOnly = true)
    public byte[] prescriptionPdf(Long prescriptionId) {
        Prescription rx = prescriptions.findById(prescriptionId)
                .filter(p -> p.getPatient().getId().equals(me()) && p.getStatus() != PrescriptionStatus.DRAFT)
                .orElseThrow(() -> ApiException.notFound("Prescription", prescriptionId));
        return pdfService.prescription(patientMapper.toDto(rx.getPatient()), medicationMapper.toDto(rx));
    }

    // ----- appointments -----

    @Transactional(readOnly = true)
    public List<AppointmentDto> appointments() {
        return schedulingService.forPatient(me());
    }

    @Transactional(readOnly = true)
    public List<SlotDto> availableSlots(Long providerId, LocalDate date) {
        return schedulingService.slots(providerId, date, true);
    }

    @Transactional
    public AppointmentDto book(PortalBookRequest request) {
        return schedulingService.book(me(), request.slotId(), request.appointmentType(), request.reason(), BookedVia.PORTAL);
    }

    @Transactional
    public AppointmentDto cancel(Long appointmentId, String reason) {
        return schedulingService.cancel(appointmentId, reason, me());
    }

    // ----- helpers -----

    private Encounter ownEncounter(Long encounterId) {
        return encounters.findById(encounterId)
                .filter(e -> e.getPatient().getId().equals(me()) && e.getStatus() != EncounterStatus.CANCELLED)
                .orElseThrow(() -> ApiException.notFound("Visit", encounterId));
    }

    /** Builds the patient-facing summary from signed notes only; drafts are never shown to patients. */
    private VisitSummaryDto summary(Encounter e) {
        List<String> dx = diagnoses.findByEncounterIdOrderByDiagnosisTypeAscIdAsc(e.getId()).stream()
                .map(d -> d.getIcd10().getDescription() + " (" + d.getIcd10().getCode() + ")"
                        + (d.getDiagnosisType() == DiagnosisType.PRIMARY ? " - primary" : ""))
                .toList();
        List<ClinicalNote> signed = notes.findByEncounterIdOrderByCreatedAtAsc(e.getId()).stream()
                .filter(n -> n.getStatus() == NoteStatus.SIGNED).toList();
        String assessment = join(signed.stream().map(ClinicalNote::getAssessment).toList());
        String plan = join(signed.stream().map(ClinicalNote::getPlan).toList());
        List<String> meds = prescriptions.findByPatientIdOrderByCreatedAtDesc(e.getPatient().getId()).stream()
                .filter(p -> p.getEncounter().getId().equals(e.getId()) && p.getStatus() != PrescriptionStatus.DRAFT)
                .map(p -> p.getDrug().getName() + " - " + p.getDose() + ", " + p.getFrequency() + ", " + p.getDurationDays()
                        + " days" + (p.getStatus() == PrescriptionStatus.DISCONTINUED ? " (discontinued)" : ""))
                .toList();
        VitalDto vital = vitals.findByEncounterIdOrderByRecordedAtDesc(e.getId()).stream().findFirst()
                .map(clinicalMapper::toDto).orElse(null);
        return new VisitSummaryDto(e.getId(), e.getEncounterDate(), e.getProvider().getFullName(),
                e.getDepartment() == null ? null : e.getDepartment().getName(), e.getEncounterType(), e.getStatus(),
                e.getChiefComplaint(), dx, assessment, plan, meds, vital);
    }

    private static String join(List<String> parts) {
        String joined = parts.stream().filter(Objects::nonNull).filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));
        return joined.isBlank() ? null : joined;
    }

    private static Long me() {
        return SecurityUtils.currentPatientId();
    }
}
