package com.careconnect.user.service;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.user.dto.UserDtos.CreateUserRequest;
import com.careconnect.user.dto.UserDtos.DepartmentDto;
import com.careconnect.user.dto.UserDtos.DepartmentRequest;
import com.careconnect.user.dto.UserDtos.ProviderDto;
import com.careconnect.user.dto.UserDtos.UpdateUserRequest;
import com.careconnect.user.dto.UserDtos.UserDto;
import com.careconnect.user.mapper.UserMapper;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Department;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import com.careconnect.user.repository.DepartmentRepository;
import com.careconnect.auth.repository.RefreshTokenRepository;
import com.careconnect.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final PatientRepository patients;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(String search, Role role, Pageable pageable) {
        String term = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";
        return PageResponse.of(users.search(term, role, pageable), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username())) {
            throw ApiException.conflict("USERNAME_TAKEN", "Username '" + request.username() + "' is already in use.");
        }
        if (request.email() != null && !request.email().isBlank() && users.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email is already in use.");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        apply(user, request.email(), request.fullName(), request.role(), request.departmentId(), request.phone(),
                request.specialty(), request.licenseNo(), request.patientId());
        return mapper.toDto(users.save(user));
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest request) {
        AppUser user = find(id);
        apply(user, request.email(), request.fullName(), request.role(), request.departmentId(), request.phone(),
                request.specialty(), request.licenseNo(), request.patientId());
        return mapper.toDto(user);
    }

    @Transactional
    public UserDto setActive(Long id, boolean active) {
        AppUser user = find(id);
        if (!active && Objects.equals(user.getId(), SecurityUtils.currentUser().id())) {
            throw ApiException.badRequest("You cannot deactivate your own account.");
        }
        user.setActive(active);
        if (!active) {
            refreshTokens.revokeAllForUser(user.getId());
        }
        return mapper.toDto(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        AppUser user = find(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokens.revokeAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public List<ProviderDto> providers(Long departmentId) {
        return users.findByRoleAndActiveTrueOrderByFullNameAsc(Role.DOCTOR).stream()
                .filter(u -> departmentId == null
                        || (u.getDepartment() != null && departmentId.equals(u.getDepartment().getId())))
                .map(mapper::toProvider)
                .toList();
    }

    // ----- departments -----

    @Transactional(readOnly = true)
    public List<DepartmentDto> departments() {
        return departments.findAllByOrderByNameAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public DepartmentDto createDepartment(DepartmentRequest request) {
        if (departments.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("DEPARTMENT_EXISTS", "Department code " + request.code() + " already exists.");
        }
        Department department = new Department();
        applyDepartment(department, request);
        return mapper.toDto(departments.save(department));
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentRequest request) {
        Department department = departments.findById(id).orElseThrow(() -> ApiException.notFound("Department", id));
        if (!department.getCode().equalsIgnoreCase(request.code()) && departments.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("DEPARTMENT_EXISTS", "Department code " + request.code() + " already exists.");
        }
        applyDepartment(department, request);
        return mapper.toDto(department);
    }

    private void applyDepartment(Department department, DepartmentRequest request) {
        department.setCode(request.code().trim().toUpperCase());
        department.setName(request.name().trim());
        department.setDescription(request.description());
        department.setActive(request.active());
    }

    private void apply(AppUser user, String email, String fullName, Role role, Long departmentId, String phone,
                       String specialty, String licenseNo, Long patientId) {
        user.setEmail(email == null || email.isBlank() ? null : email.trim().toLowerCase());
        user.setFullName(fullName.trim());
        user.setRole(role);
        user.setPhone(phone);
        user.setSpecialty(specialty);
        user.setLicenseNo(licenseNo);
        user.setDepartment(departmentId == null ? null
                : departments.findById(departmentId).orElseThrow(() -> ApiException.notFound("Department", departmentId)));

        // A PATIENT login must point at exactly one patient record; staff accounts never do.
        if (role == Role.PATIENT) {
            if (patientId == null) {
                throw ApiException.badRequest("A patient account must be linked to a patient record.");
            }
            Patient patient = patients.findByIdAndDeletedFalse(patientId)
                    .orElseThrow(() -> ApiException.notFound("Patient", patientId));
            boolean alreadyLinked = users.findByPatientId(patientId)
                    .filter(existing -> !existing.getId().equals(user.getId())).isPresent();
            if (alreadyLinked) {
                throw ApiException.conflict("PATIENT_ALREADY_LINKED", "This patient already has a portal account.");
            }
            user.setPatient(patient);
            user.setDepartment(null);
        } else {
            user.setPatient(null);
        }
    }

    private AppUser find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
    }
}
