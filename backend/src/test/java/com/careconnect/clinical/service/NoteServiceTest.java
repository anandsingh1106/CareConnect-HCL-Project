package com.careconnect.clinical.service;

import com.careconnect.clinical.dto.ClinicalDtos.NoteRequest;
import com.careconnect.clinical.mapper.ClinicalMapper;
import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteType;
import com.careconnect.clinical.model.ClinicalNote;
import com.careconnect.clinical.repository.ClinicalNoteRepository;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.common.exception.ApiException;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock ClinicalNoteRepository notes;
    @Mock EncounterRepository encounters;
    @Mock PatientRepository patients;
    @Mock AppUserRepository users;
    @Mock ClinicalMapper mapper;
    @InjectMocks NoteService service;

    private AppUser author;
    private ClinicalNote note;

    @BeforeEach
    void setUp() {
        author = new AppUser();
        author.setId(2L);
        Patient patient = new Patient();
        patient.setId(1L);
        note = new ClinicalNote();
        note.setId(10L);
        note.setAuthor(author);
        note.setPatient(patient);
        note.setStatus(NoteStatus.DRAFT);
        note.setAssessment("Viral fever");
        loginAs(2L);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorCanSignADraft() {
        when(notes.findById(10L)).thenReturn(Optional.of(note));
        when(users.findById(2L)).thenReturn(Optional.of(author));

        service.sign(10L);

        assertThat(note.getStatus()).isEqualTo(NoteStatus.SIGNED);
        assertThat(note.getSignedAt()).isNotNull();
        assertThat(note.getSignedBy()).isSameAs(author);
    }

    @Test
    void signedNoteCannotBeEdited() {
        note.setStatus(NoteStatus.SIGNED);
        when(notes.findById(10L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> service.update(10L, new NoteRequest(null, "changed", null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "NOTE_SIGNED");
    }

    @Test
    void anotherDoctorCannotEditTheDraft() {
        loginAs(3L);
        when(notes.findById(10L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> service.update(10L, new NoteRequest(null, "x", null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "FORBIDDEN");
    }

    @Test
    void addendumIsSignedImmediatelyAndPointsToTheOriginal() {
        note.setStatus(NoteStatus.SIGNED);
        when(notes.findById(10L)).thenReturn(Optional.of(note));
        when(users.findById(2L)).thenReturn(Optional.of(author));

        service.addendum(10L, new NoteRequest(null, null, null, null, "Repeat CBC in 3 days", null));

        ArgumentCaptor<ClinicalNote> saved = ArgumentCaptor.forClass(ClinicalNote.class);
        verify(notes).save(saved.capture());
        assertThat(saved.getValue().getNoteType()).isEqualTo(NoteType.ADDENDUM);
        assertThat(saved.getValue().getStatus()).isEqualTo(NoteStatus.SIGNED);
        assertThat(saved.getValue().getParentNote()).isSameAs(note);
    }

    private static void loginAs(Long userId) {
        UserPrincipal doctor = new UserPrincipal(userId, "doctor" + userId, "Doctor", Role.DOCTOR, null, null, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(doctor, null, doctor.getAuthorities()));
    }
}
