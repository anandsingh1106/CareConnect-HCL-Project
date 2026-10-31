package com.careconnect.user.repository;

import com.careconnect.user.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
