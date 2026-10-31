package com.careconnect.order.repository;

import com.careconnect.order.model.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    List<LabTest> findAllByOrderByNameAsc();

    List<LabTest> findByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
