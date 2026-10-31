package com.careconnect.medication.service;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.medication.dto.MedicationDtos.DrugDto;
import com.careconnect.medication.dto.MedicationDtos.DrugRequest;
import com.careconnect.medication.dto.MedicationDtos.InteractionDto;
import com.careconnect.medication.dto.MedicationDtos.InteractionRequest;
import com.careconnect.medication.dto.MedicationDtos.InventoryTransactionDto;
import com.careconnect.medication.dto.MedicationDtos.StockAdjustmentRequest;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.DrugInteraction;
import com.careconnect.medication.model.InventoryTransaction;
import com.careconnect.medication.model.MedicationEnums.InventoryTxnType;
import com.careconnect.medication.repository.DrugInteractionRepository;
import com.careconnect.medication.repository.DrugRepository;
import com.careconnect.medication.repository.InventoryTransactionRepository;
import com.careconnect.notification.service.NotificationService;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Drug master, stock movements and the drug interaction table. */
@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugRepository drugs;
    private final InventoryTransactionRepository inventory;
    private final DrugInteractionRepository interactions;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final MedicationMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<DrugDto> search(String q, boolean activeOnly, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return PageResponse.of(drugs.search(term, activeOnly ? Boolean.TRUE : null, pageable), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public DrugDto get(Long id) {
        return mapper.toDto(find(id));
    }

    @Transactional(readOnly = true)
    public List<DrugDto> lowStock() {
        return drugs.findLowStock().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public DrugDto create(DrugRequest request) {
        if (drugs.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("CODE_EXISTS", "Drug code " + request.code() + " already exists.");
        }
        Drug drug = new Drug();
        drug.setStockQty(0);
        apply(drug, request);
        return mapper.toDto(drugs.save(drug));
    }

    @Transactional
    public DrugDto update(Long id, DrugRequest request) {
        Drug drug = find(id);
        if (!drug.getCode().equalsIgnoreCase(request.code()) && drugs.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("CODE_EXISTS", "Drug code " + request.code() + " already exists.");
        }
        apply(drug, request);
        return mapper.toDto(drug);
    }

    /** Receives stock (positive) or corrects it (positive or negative adjustment). Stock can never go below zero. */
    @Transactional
    public DrugDto adjustStock(Long id, StockAdjustmentRequest request) {
        if (request.txnType() == InventoryTxnType.DISPENSE) {
            throw ApiException.badRequest("Dispensing is recorded through the prescription workflow.");
        }
        if (request.changeQty() == 0 || (request.txnType() == InventoryTxnType.RECEIPT && request.changeQty() < 0)) {
            throw ApiException.badRequest("A receipt must add a positive quantity; an adjustment cannot be zero.");
        }
        Drug drug = find(id);
        int balance = drug.getStockQty() + request.changeQty();
        if (balance < 0) {
            throw ApiException.badRequest("Stock cannot go below zero (current stock " + drug.getStockQty() + ").");
        }
        drug.setStockQty(balance);

        InventoryTransaction txn = new InventoryTransaction();
        txn.setDrug(drug);
        txn.setChangeQty(request.changeQty());
        txn.setBalanceAfter(balance);
        txn.setTxnType(request.txnType());
        txn.setReference(request.reference());
        txn.setPerformedBy(users.require(SecurityUtils.currentUser().id()));
        txn.setCreatedAt(LocalDateTime.now());
        inventory.save(txn);

        if (drug.isLowStock()) {
            notifications.notifyRole(Role.PHARMACIST, "Low stock: " + drug.getName(),
                    drug.getName() + " has " + balance + " units left (reorder level " + drug.getReorderLevel() + ").",
                    NotificationService.TYPE_LOW_STOCK, "/pharmacy/inventory");
        }
        return mapper.toDto(drug);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDto> transactions(Long drugId) {
        find(drugId);
        return inventory.findTop50ByDrugIdOrderByCreatedAtDesc(drugId).stream().map(mapper::toDto).toList();
    }

    // ----- interactions -----

    @Transactional(readOnly = true)
    public List<InteractionDto> interactions(Long drugId) {
        List<DrugInteraction> list = drugId == null ? interactions.findAllOrdered() : interactions.findForDrug(drugId);
        return list.stream().map(mapper::toDto).toList();
    }

    @Transactional
    public InteractionDto addInteraction(InteractionRequest request) {
        if (request.drugAId().equals(request.drugBId())) {
            throw ApiException.badRequest("Choose two different drugs.");
        }
        if (interactions.existsPair(request.drugAId(), request.drugBId())) {
            throw ApiException.conflict("INTERACTION_EXISTS", "An interaction between these drugs is already recorded.");
        }
        DrugInteraction interaction = new DrugInteraction();
        interaction.setDrugA(find(request.drugAId()));
        interaction.setDrugB(find(request.drugBId()));
        interaction.setSeverity(request.severity());
        interaction.setDescription(request.description().trim());
        return mapper.toDto(interactions.save(interaction));
    }

    @Transactional
    public void deleteInteraction(Long id) {
        if (!interactions.existsById(id)) {
            throw ApiException.notFound("Interaction", id);
        }
        interactions.deleteById(id);
    }

    private void apply(Drug drug, DrugRequest request) {
        drug.setCode(request.code().trim().toUpperCase());
        drug.setName(request.name().trim());
        drug.setGenericName(request.genericName().trim());
        drug.setDrugClass(request.drugClass());
        drug.setDosageForm(request.dosageForm().trim().toUpperCase());
        drug.setStrength(request.strength());
        drug.setDefaultRoute(request.defaultRoute());
        drug.setUnitPrice(request.unitPrice());
        drug.setReorderLevel(request.reorderLevel());
        drug.setActive(request.active());
    }

    private Drug find(Long id) {
        return drugs.findById(id).orElseThrow(() -> ApiException.notFound("Drug", id));
    }
}
