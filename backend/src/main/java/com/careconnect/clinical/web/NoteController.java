package com.careconnect.clinical.web;

import com.careconnect.clinical.dto.ClinicalDtos.NoteDto;
import com.careconnect.clinical.dto.ClinicalDtos.NoteRequest;
import com.careconnect.clinical.service.NoteService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Clinical notes (SOAP)")
@RestController
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping("/api/patients/{patientId}/notes")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<NoteDto> forPatient(@PathVariable Long patientId) {
        return noteService.forPatient(patientId);
    }

    @Operation(summary = "Start a DRAFT SOAP note on an open encounter")
    @PostMapping("/api/encounters/{encounterId}/notes")
    @PreAuthorize(Authz.DOCTOR)
    @ResponseStatus(HttpStatus.CREATED)
    public NoteDto create(@PathVariable Long encounterId, @Valid @RequestBody NoteRequest request) {
        return noteService.create(encounterId, request);
    }

    @Operation(summary = "Edit a DRAFT note (author only)")
    @PutMapping("/api/notes/{id}")
    @PreAuthorize(Authz.DOCTOR)
    public NoteDto update(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return noteService.update(id, request);
    }

    @Operation(summary = "Sign a note - it becomes read-only")
    @PostMapping("/api/notes/{id}/sign")
    @PreAuthorize(Authz.DOCTOR)
    public NoteDto sign(@PathVariable Long id) {
        return noteService.sign(id);
    }

    @Operation(summary = "Add a signed addendum to a signed note")
    @PostMapping("/api/notes/{id}/addenda")
    @PreAuthorize(Authz.DOCTOR)
    @ResponseStatus(HttpStatus.CREATED)
    public NoteDto addendum(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return noteService.addendum(id, request);
    }
}
