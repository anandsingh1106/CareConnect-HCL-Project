package com.careconnect.clinical.repository;

import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteType;
import com.careconnect.clinical.model.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    List<ClinicalNote> findByEncounterIdOrderByCreatedAtAsc(Long encounterId);

    List<ClinicalNote> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<ClinicalNote> findByEncounterIdAndStatusAndNoteType(Long encounterId, NoteStatus status, NoteType noteType);

    boolean existsByEncounterIdAndStatus(Long encounterId, NoteStatus status);

    long countByAuthorIdAndStatus(Long authorId, NoteStatus status);
}
