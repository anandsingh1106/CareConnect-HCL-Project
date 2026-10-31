package com.careconnect.patient.repository;

import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndDeletedFalse(Long id);

    /** Loads an active (not deleted) patient or fails with 404, and tags the request for the audit log. */
    default Patient require(Long id) {
        Patient patient = findByIdAndDeletedFalse(id).orElseThrow(() -> ApiException.notFound("Patient", id));
        AuditContext.setPatientId(patient.getId());
        return patient;
    }

    /**
     * Advanced search. Every filter is optional:
     * q matches name, MRN or phone; mrn / dob / phone are exact (phone: contains).
     */
    @Query("""
            select p from Patient p
            where p.deleted = false
              and (:q is null
                   or lower(p.firstName) like :q
                   or lower(p.lastName) like :q
                   or lower(concat(concat(p.firstName, ' '), p.lastName)) like :q
                   or lower(p.mrn) like :q
                   or p.phone like :q)
              and (:mrn is null or upper(p.mrn) = :mrn)
              and (:dob is null or p.dateOfBirth = :dob)
              and (:phone is null or p.phone like :phone)
            """)
    Page<Patient> search(@Param("q") String q, @Param("mrn") String mrn, @Param("dob") LocalDate dob,
                         @Param("phone") String phone, Pageable pageable);

    long countByDeletedFalse();

    long countByDeletedFalseAndCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
