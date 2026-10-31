package com.careconnect.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

/** Pre-filled SOAP text a doctor can start a note from. */
@Getter
@Setter
@Entity
@Table(name = "note_templates")
public class NoteTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "note_templates_seq")
    @SequenceGenerator(name = "note_templates_seq", sequenceName = "note_templates_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 100)
    private String specialty;

    @Column(length = 2000)
    private String subjective;

    @Column(length = 2000)
    private String objective;

    @Column(length = 2000)
    private String assessment;

    @Column(name = "plan_text", length = 2000)
    private String plan;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean active = true;
}
