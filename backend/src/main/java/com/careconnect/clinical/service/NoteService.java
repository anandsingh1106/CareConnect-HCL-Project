package com.careconnect.clinical.service;

import com.careconnect.clinical.dto.ClinicalDtos.NoteDto;
import com.careconnect.clinical.dto.ClinicalDtos.NoteRequest;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteType;
import com.careconnect.clinical.model.ClinicalNote;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.clinical.repository.ClinicalNoteRepository;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOAP notes with signing and addenda.
 * Rules: only the author edits a DRAFT; signing locks the note permanently;
 * any later change is a new ADDENDUM (signed immediately) that points to the original note.
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final ClinicalNoteRepository notes;
    private final EncounterRepository encounters;
    private final PatientRepository patients;
    private final AppUserRepository users;
    private final ClinicalMapper mapper;

    @Transactional(readOnly = true)
    public List<NoteDto> forPatient(Long patientId) {
        patients.require(patientId);
        return notes.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public NoteDto create(Long encounterId, NoteRequest request) {
        Encounter encounter = encounters.require(encounterId);
        if (encounter.getStatus() != EncounterStatus.OPEN) {
            throw ApiException.conflict("ENCOUNTER_CLOSED", "Notes can only be started on an open encounter. Use an addendum instead.");
        }
        ClinicalNote note = new ClinicalNote();
        note.setEncounter(encounter);
        note.setPatient(encounter.getPatient());
        note.setAuthor(currentUser());
        note.setNoteType(NoteType.PROGRESS);
        note.setStatus(NoteStatus.DRAFT);
        note.setTemplateCode(request.templateCode());
        applySections(note, request);
        return mapper.toDto(notes.save(note));
    }

    @Transactional
    public NoteDto update(Long noteId, NoteRequest request) {
        ClinicalNote note = find(noteId);
        requireEditableByCurrentUser(note);
        if (request.version() != null && !request.version().equals(note.getVersion())) {
            throw ApiException.conflict("CONCURRENT_UPDATE", "The note was changed in another window. Reload it first.");
        }
        applySections(note, request);
        return mapper.toDto(note);
    }

    @Transactional
    public NoteDto sign(Long noteId) {
        ClinicalNote note = find(noteId);
        requireEditableByCurrentUser(note);
        if (isBlank(note.getSubjective()) && isBlank(note.getObjective())
                && isBlank(note.getAssessment()) && isBlank(note.getPlan())) {
            throw ApiException.badRequest("An empty note cannot be signed.");
        }
        note.setStatus(NoteStatus.SIGNED);
        note.setSignedAt(LocalDateTime.now());
        note.setSignedBy(currentUser());
        return mapper.toDto(note);
    }

    /** Adds a signed addendum to a signed note (also allowed after the encounter is closed). */
    @Transactional
    public NoteDto addendum(Long noteId, NoteRequest request) {
        ClinicalNote parent = find(noteId);
        if (parent.getStatus() != NoteStatus.SIGNED) {
            throw ApiException.conflict("NOTE_NOT_SIGNED", "Draft notes can be edited directly; addenda are for signed notes.");
        }
        ClinicalNote addendum = new ClinicalNote();
        addendum.setEncounter(parent.getEncounter());
        addendum.setPatient(parent.getPatient());
        addendum.setParentNote(parent.getParentNote() != null ? parent.getParentNote() : parent);
        addendum.setNoteType(NoteType.ADDENDUM);
        AppUser author = currentUser();
        addendum.setAuthor(author);
        applySections(addendum, request);
        if (isBlank(addendum.getSubjective()) && isBlank(addendum.getObjective())
                && isBlank(addendum.getAssessment()) && isBlank(addendum.getPlan())) {
            throw ApiException.badRequest("The addendum is empty.");
        }
        addendum.setStatus(NoteStatus.SIGNED);
        addendum.setSignedAt(LocalDateTime.now());
        addendum.setSignedBy(author);
        return mapper.toDto(notes.save(addendum));
    }

    private void requireEditableByCurrentUser(ClinicalNote note) {
        if (note.getStatus() == NoteStatus.SIGNED) {
            throw ApiException.conflict("NOTE_SIGNED", "Signed notes are read-only. Add an addendum instead.");
        }
        if (!note.getAuthor().getId().equals(SecurityUtils.currentUser().id())) {
            throw ApiException.forbidden("Only the author can edit or sign a draft note.");
        }
    }

    private void applySections(ClinicalNote note, NoteRequest request) {
        note.setSubjective(request.subjective());
        note.setObjective(request.objective());
        note.setAssessment(request.assessment());
        note.setPlan(request.plan());
    }

    private ClinicalNote find(Long noteId) {
        ClinicalNote note = notes.findById(noteId).orElseThrow(() -> ApiException.notFound("Note", noteId));
        AuditContext.setPatientId(note.getPatient().getId());
        return note;
    }

    private AppUser currentUser() {
        Long id = SecurityUtils.currentUser().id();
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
