package com.careconnect.medication.dto;

import com.careconnect.medication.model.MedicationEnums.AdministrationStatus;
import com.careconnect.medication.model.MedicationEnums.InteractionSeverity;
import com.careconnect.medication.model.MedicationEnums.InventoryTxnType;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.MedicationEnums.Route;
import com.careconnect.order.dto.OrderDtos.OrderAlertDto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for prescriptions, pharmacy, MAR and the drug master. */
public final class MedicationDtos {

    private MedicationDtos() {
    }

    public record PrescriptionDto(
            Long id,
            String rxNo,
            Long orderId,
            String orderNo,
            Long patientId,
            String patientName,
            String patientMrn,
            Long encounterId,
            Long prescriberId,
            String prescriberName,
            Long drugId,
            String drugCode,
            String drugName,
            String genericName,
            String drugClass,
            Integer drugStock,
            String dose,
            Route route,
            String frequency,
            Integer durationDays,
            Integer quantity,
            String instructions,
            PrescriptionStatus status,
            boolean currentlyActive,
            String verifiedByName,
            LocalDateTime verifiedAt,
            String pharmacistNote,
            String dispensedByName,
            LocalDateTime dispensedAt,
            Integer dispensedQty,
            String discontinuedByName,
            LocalDateTime discontinuedAt,
            String discontinueReason,
            Long replacesId,
            LocalDateTime createdAt) {
    }

    /** Prescription with the decision-support alerts raised when it was ordered (for pharmacist review). */
    public record PrescriptionDetailDto(PrescriptionDto prescription, List<OrderAlertDto> alerts,
                                        List<String> patientAllergies) {
    }

    public record VerifyRequest(@Size(max = 500) String note) {
    }

    public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {
    }

    public record DispenseRequest(@Min(1) @Max(10000) Integer quantity) {
    }

    public record ModifyPrescriptionRequest(
            @NotBlank @Size(max = 50) String dose,
            @NotNull Route route,
            @NotBlank @Size(max = 30) String frequency,
            @NotNull @Min(1) @Max(365) Integer durationDays,
            @NotNull @Min(1) @Max(10000) Integer quantity,
            @Size(max = 500) String instructions,
            @Size(max = 500) String overrideReason) {
    }

    public record AdministrationDto(
            Long id,
            Long prescriptionId,
            String rxNo,
            String drugName,
            Long patientId,
            Long administeredById,
            String administeredByName,
            LocalDateTime administeredAt,
            String doseGiven,
            Route route,
            AdministrationStatus status,
            String notes) {
    }

    public record AdministrationRequest(
            @NotNull Long prescriptionId,
            @PastOrPresent LocalDateTime administeredAt,
            @Size(max = 50) String doseGiven,
            Route route,
            @NotNull AdministrationStatus status,
            @Size(max = 500) String notes) {
    }

    public record DrugDto(
            Long id,
            String code,
            String name,
            String genericName,
            String drugClass,
            String dosageForm,
            String strength,
            String defaultRoute,
            BigDecimal unitPrice,
            Integer stockQty,
            Integer reorderLevel,
            boolean lowStock,
            boolean active) {
    }

    public record DrugRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Use capital letters, digits, - or _.") String code,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String genericName,
            @Size(max = 100) String drugClass,
            @NotBlank @Size(max = 30) String dosageForm,
            @Size(max = 40) String strength,
            @Size(max = 20) String defaultRoute,
            @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            @NotNull @Min(0) Integer reorderLevel,
            boolean active) {
    }

    public record StockAdjustmentRequest(
            @NotNull Integer changeQty,
            @NotNull InventoryTxnType txnType,
            @Size(max = 100) String reference) {
    }

    public record InventoryTransactionDto(Long id, Long drugId, Integer changeQty, Integer balanceAfter,
                                          InventoryTxnType txnType, String reference, String performedByName,
                                          LocalDateTime createdAt) {
    }

    public record InteractionDto(Long id, Long drugAId, String drugAName, Long drugBId, String drugBName,
                                 InteractionSeverity severity, String description) {
    }

    public record InteractionRequest(
            @NotNull Long drugAId,
            @NotNull Long drugBId,
            @NotNull InteractionSeverity severity,
            @NotBlank @Size(max = 500) String description) {
    }
}
