package com.careconnect.clinical.service;

import com.careconnect.clinical.dto.ClinicalDtos.DiagnosisDto;
import com.careconnect.clinical.dto.ClinicalDtos.DiagnosisRequest;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterDetailDto;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterDto;
import com.careconnect.clinical.dto.ClinicalDtos.EncounterRequest;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.ClinicalEnums.DiagnosisType;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.clinical.model.EncounterDiagnosis;
import com.careconnect.clinical.model.Icd10Code;
import com.careconnect.clinical.repository.ClinicalNoteRepository;
import com.careconnect.clinical.repository.EncounterDiagnosisRepository;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.clinical.repository.Icd10Repository;
import com.careconnect.clinical.repository.VitalRepository;
import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.scheduling.model.Appointment;
import com.careconnect.scheduling.model.SchedulingEnums.AppointmentStatus;
import com.careconnect.scheduling.repository.AppointmentRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Encounters (visits) and their ICD-10 diagnoses. */
@Service
@RequiredArgsConstructor
public class EncounterService {

    private final EncounterRepository encounters;
    private final EncounterDiagnosisRepository diagnoses;
    private final ClinicalNoteRepository notes;
    private final VitalRepository vitals;
    private final Icd10Repository icd10;
    private final PatientRepository patients;
    private final AppUserRepository users;
    private final AppointmentRepository appointments;
    private final ClinicalMapper mapper;

    @Transactional
    public EncounterDto open(EncounterRequest request) {
        UserPrincipal current = SecurityUtils.currentUser();
        Patient patient = patients.require(request.patientId());

        Long providerId = request.providerId();
        if (providerId == null) {
            if (!current.hasRole(Role.DOCTOR)) {
                throw ApiException.badRequest("Select the doctor responsible for this encounter.");
            }
            providerId = current.id();
        }
        Long finalProviderId = providerId;
        AppUser provider = users.findById(providerId).orElseThrow(() -> ApiException.notFound("Provider", finalProviderId));
        if (provider.getRole() != Role.DOCTOR || !provider.isActive()) {
            throw ApiException.badRequest("The responsible provider must be an active doctor.");
        }

        Encounter encounter = new Encounter();
        encounter.setPatient(patient);
        encounter.setProvider(provider);
        encounter.setDepartment(provider.getDepartment());
        encounter.setEncounterType(request.encounterType());
        encounter.setChiefComplaint(request.chiefComplaint());
        encounter.setEncounterDate(LocalDateTime.now());
        encounter.setStatus(EncounterStatus.OPEN);

        if (request.appointmentId() != null) {
            Appointment appointment = appointments.findById(request.appointmentId())
                    .orElseThrow(() -> ApiException.notFound("Appointment", request.appointmentId()));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw ApiException.badRequest("The appointment belongs to a different patient.");
            }
            if (appointment.getStatus() == AppointmentStatus.BOOKED) {
                appointment.setStatus(AppointmentStatus.CHECKED_IN);
            }
            encounter.setAppointment(appointment);
        }
        return mapper.toDto(encounters.save(encounter), null);
    }

    @Transactional(readOnly = true)
    public EncounterDetailDto detail(Long encounterId) {
        Encounter encounter = encounters.require(encounterId);
        List<DiagnosisDto> dx = diagnoses.findByEncounterIdOrderByDiagnosisTypeAscIdAsc(encounterId).stream()
                .map(mapper::toDto).toList();
        return new EncounterDetailDto(
                mapper.toDto(encounter, primaryLabel(dx)),
                notes.findByEncounterIdOrderByCreatedAtAsc(encounterId).stream().map(mapper::toDto).toList(),
                dx,
                vitals.findByEncounterIdOrderByRecordedAtDesc(encounterId).stream().map(mapper::toDto).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<EncounterDto> forPatient(Long patientId, Pageable pageable) {
        patients.require(patientId);
        Page<Encounter> page = encounters.findByPatientIdOrderByEncounterDateDesc(patientId, pageable);
        Map<Long, String> primary = primaryDiagnoses(page.getContent());
        return PageResponse.of(page.map(e -> mapper.toDto(e, primary.get(e.getId()))));
    }

    /** Doctor's worklist: own encounters with the given status (default OPEN). Nurses see all open encounters. */
    @Transactional(readOnly = true)
    public List<EncounterDto> worklist(EncounterStatus status) {
        UserPrincipal current = SecurityUtils.currentUser();
        EncounterStatus wanted = status == null ? EncounterStatus.OPEN : status;
        List<Encounter> list = current.hasRole(Role.DOCTOR)
                ? encounters.findByProviderIdAndStatusOrderByEncounterDateDesc(current.id(), wanted)
                : encounters.findByStatusOrderByEncounterDateDesc(wanted);
        Map<Long, String> primary = primaryDiagnoses(list);
        return list.stream().map(e -> mapper.toDto(e, primary.get(e.getId()))).toList();
    }

    /** Closing requires every note to be signed; a linked appointment is marked completed. */
    @Transactional
    public EncounterDto close(Long encounterId) {
        Encounter encounter = encounters.require(encounterId);
        requireOpen(encounter);
        if (notes.existsByEncounterIdAndStatus(encounterId, NoteStatus.DRAFT)) {
            throw ApiException.conflict("UNSIGNED_NOTES", "Sign all draft notes before closing the encounter.");
        }
        encounter.setStatus(EncounterStatus.CLOSED);
        encounter.setClosedAt(LocalDateTime.now());
        if (encounter.getAppointment() != null) {
            encounter.getAppointment().setStatus(AppointmentStatus.COMPLETED);
        }
        return mapper.toDto(encounter, primaryDiagnoses(List.of(encounter)).get(encounterId));
    }

    // ----- diagnoses -----

    @Transactional
    public DiagnosisDto addDiagnosis(Long encounterId, DiagnosisRequest request) {
        Encounter encounter = encounters.require(encounterId);
        requireOpen(encounter);
        String code = request.icd10Code().trim().toUpperCase();
        Icd10Code icd = icd10.findById(code).orElseThrow(() -> ApiException.badRequest("Unknown ICD-10 code " + code));
        if (diagnoses.existsByEncounterIdAndIcd10Code(encounterId, code)) {
            throw ApiException.conflict("DUPLICATE_DIAGNOSIS", code + " is already recorded for this encounter.");
        }
        DiagnosisType type = request.diagnosisType() == null ? DiagnosisType.SECONDARY : request.diagnosisType();
        if (type == DiagnosisType.PRIMARY) {
            // Only one primary diagnosis per encounter: demote the previous one.
            diagnoses.findByEncounterIdAndDiagnosisType(encounterId, DiagnosisType.PRIMARY)
                    .forEach(d -> d.setDiagnosisType(DiagnosisType.SECONDARY));
        }
        EncounterDiagnosis diagnosis = new EncounterDiagnosis();
        diagnosis.setEncounter(encounter);
        diagnosis.setIcd10(icd);
        diagnosis.setDiagnosisType(type);
        diagnosis.setNotes(request.notes());
        return mapper.toDto(diagnoses.save(diagnosis));
    }

    @Transactional
    public void removeDiagnosis(Long diagnosisId) {
        EncounterDiagnosis diagnosis = diagnoses.findById(diagnosisId)
                .orElseThrow(() -> ApiException.notFound("Diagnosis", diagnosisId));
        AuditContext.setPatientId(diagnosis.getEncounter().getPatient().getId());
        requireOpen(diagnosis.getEncounter());
        diagnoses.delete(diagnosis);
    }

    private void requireOpen(Encounter encounter) {
        if (encounter.getStatus() != EncounterStatus.OPEN) {
            throw ApiException.conflict("ENCOUNTER_CLOSED", "This encounter is " + encounter.getStatus().name().toLowerCase()
                    + " and can no longer be changed.");
        }
    }

    private Map<Long, String> primaryDiagnoses(List<Encounter> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        return diagnoses.findByEncounterIdIn(list.stream().map(Encounter::getId).toList()).stream()
                .filter(d -> d.getDiagnosisType() == DiagnosisType.PRIMARY)
                .collect(Collectors.toMap(d -> d.getEncounter().getId(),
                        d -> d.getIcd10().getCode() + " " + d.getIcd10().getDescription(), (a, b) -> a));
    }

    private static String primaryLabel(List<DiagnosisDto> dx) {
        return dx.stream().filter(d -> d.diagnosisType() == DiagnosisType.PRIMARY)
                .map(d -> d.icd10Code() + " " + d.description()).findFirst().orElse(null);
    }
}
