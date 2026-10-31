package com.careconnect.medication.service;

import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.medication.dto.MedicationDtos.ModifyPrescriptionRequest;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDetailDto;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.InventoryTransaction;
import com.careconnect.medication.model.MedicationEnums.InventoryTxnType;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.InventoryTransactionRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.notification.service.NotificationService;
import com.careconnect.order.dto.OrderDtos.MedicationOrderDetails;
import com.careconnect.order.dto.OrderDtos.OrderDetailDto;
import com.careconnect.order.dto.OrderDtos.OrderRequest;
import com.careconnect.order.mapper.OrderMapper;
import com.careconnect.order.model.ClinicalOrder;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.repository.OrderAlertRepository;
import com.careconnect.order.service.OrderService;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import com.careconnect.patient.repository.AllergyRepository;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Pharmacy workflow on prescriptions:
 * PENDING_VERIFICATION --verify--> VERIFIED --dispense--> DISPENSED
 * PENDING_VERIFICATION --reject--> REJECTED ; any active state --discontinue (doctor)--> DISCONTINUED.
 * The linked order follows: verify -> IN_PROGRESS, dispense -> COMPLETED, reject/discontinue -> CANCELLED.
 */
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptions;
    private final InventoryTransactionRepository inventory;
    private final OrderAlertRepository alerts;
    private final AllergyRepository allergies;
    private final PatientRepository patients;
    private final AppUserRepository users;
    private final OrderService orderService;
    private final NotificationService notifications;
    private final MedicationMapper mapper;
    private final OrderMapper orderMapper;

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<PrescriptionDto> search(PrescriptionStatus status, Long patientId, Pageable pageable) {
        return PageResponse.of(prescriptions.search(status, patientId, pageable), mapper::toDto);
    }

    @Transactional(readOnly = true)
    public PrescriptionDetailDto detail(Long id) {
        Prescription prescription = prescriptions.require(id);
        List<String> patientAllergies = allergies.findByPatientIdAndStatus(prescription.getPatient().getId(), RecordStatus.ACTIVE)
                .stream().map(a -> a.getAllergen() + " (" + a.getSeverity() + ")").toList();
        return new PrescriptionDetailDto(mapper.toDto(prescription),
                alerts.findByOrderIdOrderByIdAsc(prescription.getOrder().getId()).stream().map(orderMapper::toDto).toList(),
                patientAllergies);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDto> forPatient(Long patientId, boolean activeOnly) {
        patients.require(patientId);
        return prescriptions.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .filter(p -> !activeOnly || p.isCurrentlyActive())
                .map(mapper::toDto)
                .toList();
    }

    // ------------------------------------------------------------------ pharmacist actions

    @Transactional
    public PrescriptionDto verify(Long id, String note) {
        Prescription prescription = prescriptions.require(id);
        requireStatus(prescription, PrescriptionStatus.PENDING_VERIFICATION, "verified");
        prescription.setStatus(PrescriptionStatus.VERIFIED);
        prescription.setVerifiedBy(currentUser());
        prescription.setVerifiedAt(LocalDateTime.now());
        prescription.setPharmacistNote(note);
        orderService.transition(prescription.getOrder(), OrderStatus.IN_PROGRESS);
        return mapper.toDto(prescription);
    }

    @Transactional
    public PrescriptionDto reject(Long id, String reason) {
        Prescription prescription = prescriptions.require(id);
        requireStatus(prescription, PrescriptionStatus.PENDING_VERIFICATION, "rejected");
        prescription.setStatus(PrescriptionStatus.REJECTED);
        prescription.setVerifiedBy(currentUser());
        prescription.setVerifiedAt(LocalDateTime.now());
        prescription.setPharmacistNote(reason.trim());
        ClinicalOrder order = prescription.getOrder();
        orderService.transition(order, OrderStatus.CANCELLED);
        order.setCancelReason("Rejected by pharmacist: " + reason.trim());

        notifications.notifyUser(prescription.getPrescriber().getId(), "Prescription rejected",
                prescription.getRxNo() + " " + prescription.getDrug().getName() + " for "
                        + prescription.getPatient().getFullName() + " was rejected: " + reason.trim(),
                NotificationService.TYPE_PRESCRIPTION, "/patients/" + prescription.getPatient().getId());
        return mapper.toDto(prescription);
    }

    /**
     * Dispenses a verified prescription: checks stock, decrements it, writes an inventory ledger row and
     * completes the order. Drug.version (optimistic lock) prevents two concurrent dispenses from both
     * reading the same stock level.
     */
    @Transactional
    public PrescriptionDto dispense(Long id, Integer quantity) {
        Prescription prescription = prescriptions.require(id);
        requireStatus(prescription, PrescriptionStatus.VERIFIED, "dispensed");
        int qty = quantity == null ? prescription.getQuantity() : quantity;
        Drug drug = prescription.getDrug();
        if (drug.getStockQty() < qty) {
            throw ApiException.conflict("INSUFFICIENT_STOCK", "Only " + drug.getStockQty() + " units of "
                    + drug.getName() + " are in stock; " + qty + " needed.");
        }
        AppUser pharmacist = currentUser();
        drug.setStockQty(drug.getStockQty() - qty);

        InventoryTransaction txn = new InventoryTransaction();
        txn.setDrug(drug);
        txn.setChangeQty(-qty);
        txn.setBalanceAfter(drug.getStockQty());
        txn.setTxnType(InventoryTxnType.DISPENSE);
        txn.setReference(prescription.getRxNo());
        txn.setPerformedBy(pharmacist);
        txn.setCreatedAt(LocalDateTime.now());
        inventory.save(txn);

        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setDispensedBy(pharmacist);
        prescription.setDispensedAt(LocalDateTime.now());
        prescription.setDispensedQty(qty);
        orderService.transition(prescription.getOrder(), OrderStatus.COMPLETED);

        if (drug.isLowStock()) {
            notifications.notifyRole(Role.PHARMACIST, "Low stock: " + drug.getName(),
                    drug.getName() + " has " + drug.getStockQty() + " units left (reorder level " + drug.getReorderLevel() + ").",
                    NotificationService.TYPE_LOW_STOCK, "/pharmacy/inventory");
        }
        return mapper.toDto(prescription);
    }

    // ------------------------------------------------------------------ doctor actions

    @Transactional
    public PrescriptionDto discontinue(Long id, String reason) {
        Prescription prescription = prescriptions.require(id);
        discontinueInternal(prescription, reason.trim());
        return mapper.toDto(prescription);
    }

    /**
     * "Modify" = discontinue the current prescription and create a new medication order with the new
     * dose/frequency. The new order goes through decision support and pharmacist verification again.
     */
    @Transactional
    public OrderDetailDto modify(Long id, ModifyPrescriptionRequest request) {
        Prescription old = prescriptions.require(id);
        if (!EnumSet.of(PrescriptionStatus.PENDING_VERIFICATION, PrescriptionStatus.VERIFIED, PrescriptionStatus.DISPENSED)
                .contains(old.getStatus())) {
            throw ApiException.conflict("INVALID_STATUS", "Only active prescriptions can be modified.");
        }
        discontinueInternal(old, "Modified - replaced by a new prescription");
        prescriptions.flush();

        OrderRequest orderRequest = new OrderRequest(old.getPatient().getId(), old.getEncounter().getId(),
                OrderType.MEDICATION, old.getOrder().getPriority(), null, null, null, null, null,
                old.getOrder().getClinicalIndication(), old.getOrder().getInstructions(),
                new MedicationOrderDetails(old.getDrug().getId(), request.dose(), request.route(), request.frequency(),
                        request.durationDays(), request.quantity(), request.instructions()),
                false, request.overrideReason());
        return orderService.createReplacement(orderRequest, old);
    }

    private void discontinueInternal(Prescription prescription, String reason) {
        if (!EnumSet.of(PrescriptionStatus.DRAFT, PrescriptionStatus.PENDING_VERIFICATION,
                PrescriptionStatus.VERIFIED, PrescriptionStatus.DISPENSED).contains(prescription.getStatus())) {
            throw ApiException.conflict("INVALID_STATUS", "This prescription is already " + prescription.getStatus() + ".");
        }
        prescription.setStatus(PrescriptionStatus.DISCONTINUED);
        prescription.setDiscontinuedBy(currentUser());
        prescription.setDiscontinuedAt(LocalDateTime.now());
        prescription.setDiscontinueReason(reason);
        ClinicalOrder order = prescription.getOrder();
        if (!order.getStatus().isFinal()) {
            orderService.transition(order, OrderStatus.CANCELLED);
            order.setCancelReason("Prescription discontinued: " + reason);
        }
    }

    private void requireStatus(Prescription prescription, PrescriptionStatus expected, String action) {
        if (prescription.getStatus() != expected) {
            throw ApiException.conflict("INVALID_STATUS", "Only " + expected.name().replace('_', ' ').toLowerCase()
                    + " prescriptions can be " + action + " (this one is " + prescription.getStatus() + ").");
        }
    }

    private AppUser currentUser() {
        return users.require(SecurityUtils.currentUser().id());
    }
}
