package com.careconnect.medication.model;

import com.careconnect.medication.model.MedicationEnums.InteractionSeverity;
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

/** A known interaction between two drugs (checked in both directions). */
@Getter
@Setter
@Entity
@Table(name = "drug_interactions")
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "drug_interactions_seq")
    @SequenceGenerator(name = "drug_interactions_seq", sequenceName = "drug_interactions_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_a_id", nullable = false)
    private Drug drugA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_b_id", nullable = false)
    private Drug drugB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InteractionSeverity severity;

    @Column(nullable = false, length = 500)
    private String description;
}
