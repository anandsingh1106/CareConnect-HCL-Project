package com.careconnect.medication.service;

import com.careconnect.common.exception.ApiException;
import com.careconnect.medication.dto.MedicationDtos.AdministrationDto;
import com.careconnect.medication.dto.MedicationDtos.AdministrationRequest;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.MedicationAdministration;
import com.careconnect.medication.model.MedicationEnums.AdministrationStatus;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.MedicationAdministrationRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/** Medication Administration Record: nurses document each dose given, held, refused or missed. */
@Service
@RequiredArgsConstructor
public class MarService {

    private static final EnumSet<PrescriptionStatus> ADMINISTRABLE = EnumSet.of(PrescriptionStatus.VERIFIED, PrescriptionStatus.DISPENSED);

    private final MedicationAdministrationRepository administrations;
    private final PrescriptionRepository prescriptions;
    private final PatientRepository patients;
    private final AppUserRepository users;
    private final MedicationMapper mapper;

    /** Medicines a nurse may administer now: pharmacist-verified and still within their duration. */
    @Transactional(readOnly = true)
    public List<PrescriptionDto> administrable(Long patientId) {
        patients.require(patientId);
        return prescriptions.findByPatientIdAndStatusIn(patientId, ADMINISTRABLE).stream()
                .filter(Prescription::isCurrentlyActive)
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdministrationDto> history(Long patientId) {
        patients.require(patientId);
        return administrations.findByPatientIdOrderByAdministeredAtDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public AdministrationDto record(AdministrationRequest request) {
        Prescription prescription = prescriptions.require(request.prescriptionId());
        if (!ADMINISTRABLE.contains(prescription.getStatus()) || !prescription.isCurrentlyActive()) {
            throw ApiException.conflict("NOT_ADMINISTRABLE",
                    "Only active, pharmacist-verified prescriptions can be administered.");
        }
        if (request.status() != AdministrationStatus.GIVEN && (request.notes() == null || request.notes().isBlank())) {
            throw ApiException.badRequest("Give a reason when a dose is held, refused or missed.");
        }
        MedicationAdministration entry = new MedicationAdministration();
        entry.setPrescription(prescription);
        entry.setPatient(prescription.getPatient());
        entry.setAdministeredBy(users.require(SecurityUtils.currentUser().id()));
        entry.setAdministeredAt(request.administeredAt() == null ? LocalDateTime.now() : request.administeredAt());
        entry.setDoseGiven(request.status() == AdministrationStatus.GIVEN
                ? (request.doseGiven() == null || request.doseGiven().isBlank() ? prescription.getDose() : request.doseGiven())
                : null);
        entry.setRoute(request.route() == null ? prescription.getRoute() : request.route());
        entry.setStatus(request.status());
        entry.setNotes(request.notes());
        return mapper.toDto(administrations.save(entry));
    }
}
