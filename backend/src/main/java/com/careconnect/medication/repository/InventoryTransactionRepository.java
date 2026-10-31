package com.careconnect.medication.repository;

import com.careconnect.medication.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findTop50ByDrugIdOrderByCreatedAtDesc(Long drugId);
}
