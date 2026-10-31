package com.careconnect.clinical.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;

/** ICD-10 diagnosis code (sample code table seeded by Liquibase). */
@Getter
@Setter
@Entity
@Table(name = "icd10_codes")
public class Icd10Code {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 100)
    private String category;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean active = true;
}
