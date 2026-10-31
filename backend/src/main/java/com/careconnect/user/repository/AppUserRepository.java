package com.careconnect.user.repository;

import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    default AppUser require(Long id) {
        return findById(id).orElseThrow(() -> com.careconnect.common.exception.ApiException.notFound("User", id));
    }

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPatientId(Long patientId);

    Optional<AppUser> findByPatientId(Long patientId);

    List<AppUser> findByRoleAndActiveTrueOrderByFullNameAsc(Role role);

    long countByRoleAndActiveTrue(Role role);

    long countByActiveTrue();

    @Query("""
            select u from AppUser u
            where (:role is null or u.role = :role)
              and (:search is null
                   or lower(u.username) like :search
                   or lower(u.fullName) like :search
                   or lower(u.email) like :search)
            """)
    Page<AppUser> search(@Param("search") String search, @Param("role") Role role, Pageable pageable);
}
