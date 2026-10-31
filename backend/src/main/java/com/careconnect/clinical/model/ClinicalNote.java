package com.careconnect.clinical.model;

import com.careconnect.clinical.model.ClinicalEnums.NoteStatus;
import com.careconnect.clinical.model.ClinicalEnums.NoteType;
import com.careconnect.common.model.Auditable;
import com.careconnect.patient.model.Patient;
import com.careconnect.user.model.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** SOAP note. Once SIGNED it is read-only; later changes are separate ADDENDUM notes pointing to it. */
@Getter
@Setter
@Entity
@Table(name = "clinical_notes")
public class ClinicalNote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clinical_notes_seq")
    @SequenceGenerator(name = "clinical_notes_seq", sequenceName = "clinical_notes_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 10)
    private NoteType noteType = NoteType.PROGRESS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_note_id")
    private ClinicalNote parentNote;

    @Column(name = "template_code", length = 30)
    private String templateCode;

    @Column(length = 4000)
    private String subjective;

    @Column(length = 4000)
    private String objective;

    @Column(length = 4000)
    private String assessment;

    @Column(name = "plan_text", length = 4000)
    private String plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NoteStatus status = NoteStatus.DRAFT;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by")
    private AppUser signedBy;
}
