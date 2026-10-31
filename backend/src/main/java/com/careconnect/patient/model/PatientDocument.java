package com.careconnect.patient.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.patient.model.PatientEnums.DocumentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.hibernate.type.NumericBooleanConverter;

/** Metadata of an uploaded file (lab report, scan...). The file itself lives in the upload directory. */
@Getter
@Setter
@Entity
@Table(name = "patient_documents")
public class PatientDocument extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patient_documents_seq")
    @SequenceGenerator(name = "patient_documents_seq", sequenceName = "patient_documents_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentCategory category;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** Random file name on disk; never derived from user input (prevents path traversal). */
    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(length = 500)
    private String description;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean deleted;
}
