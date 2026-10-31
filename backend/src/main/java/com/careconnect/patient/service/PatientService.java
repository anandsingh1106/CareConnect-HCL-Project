package com.careconnect.patient.service;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.service.NumberGenerator;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.patient.dto.PatientDtos.PatientRequest;
import com.careconnect.patient.dto.PatientDtos.PatientSummaryDto;
import com.careconnect.patient.mapper.PatientMapper;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patients;
    private final AppUserRepository users;
    private final NumberGenerator numberGenerator;
    private final PatientMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PatientSummaryDto> search(String q, String mrn, LocalDate dob, String phone, Pageable pageable) {
        String term = blank(q) ? null : "%" + q.trim().toLowerCase() + "%";
        String mrnExact = blank(mrn) ? null : mrn.trim().toUpperCase();
        String phoneLike = blank(phone) ? null : "%" + phone.trim() + "%";
        return PageResponse.of(patients.search(term, mrnExact, dob, phoneLike, pageable), mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PatientDto get(Long id) {
        return mapper.toDto(patients.require(id));
    }

    @Transactional
    public PatientDto register(PatientRequest request) {
        Patient patient = new Patient();
        mapper.updateEntity(request, patient);
        normalise(patient);
        patient.setMrn(numberGenerator.nextMrn());
        patient.setPrimaryProvider(resolveProvider(request.primaryProviderId()));
        return mapper.toDto(patients.save(patient));
    }

    @Transactional
    public PatientDto update(Long id, PatientRequest request) {
        Patient patient = patients.require(id);
        if (request.version() != null && !request.version().equals(patient.getVersion())) {
            throw ApiException.conflict("CONCURRENT_UPDATE",
                    "This patient was changed by someone else. Reload and try again.");
        }
        mapper.updateEntity(request, patient);
        normalise(patient);
        patient.setPrimaryProvider(resolveProvider(request.primaryProviderId()));
        return mapper.toDto(patient);
    }

    /** Soft delete: the record and its clinical history stay in the database but disappear from the application. */
    @Transactional
    public void softDelete(Long id) {
        Patient patient = patients.require(id);
        patient.setDeleted(true);
        patient.setDeletedAt(LocalDateTime.now());
        patient.setDeletedBy(SecurityUtils.currentUser().username());
    }

    private AppUser resolveProvider(Long providerId) {
        if (providerId == null) {
            return null;
        }
        AppUser provider = users.findById(providerId).orElseThrow(() -> ApiException.notFound("Provider", providerId));
        if (provider.getRole() != Role.DOCTOR) {
            throw ApiException.badRequest("The primary provider must be a doctor.");
        }
        return provider;
    }

    private void normalise(Patient patient) {
        patient.setFirstName(patient.getFirstName().trim());
        patient.setLastName(patient.getLastName().trim());
        if (patient.getEmail() != null) {
            patient.setEmail(patient.getEmail().isBlank() ? null : patient.getEmail().trim().toLowerCase());
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
