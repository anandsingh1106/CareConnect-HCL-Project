package com.careconnect.order.dto;

import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.model.MedicationEnums.Route;
import com.careconnect.order.model.OrderEnums.AlertSeverity;
import com.careconnect.order.model.OrderEnums.AlertType;
import com.careconnect.order.model.OrderEnums.OrderPriority;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.model.OrderEnums.ResultFlag;
import com.careconnect.order.model.OrderEnums.ResultStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response bodies for CPOE (orders, decision support, results). */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** Medication details, required when orderType = MEDICATION. */
    public record MedicationOrderDetails(
            @NotNull Long drugId,
            @NotBlank @Size(max = 50) String dose,
            @NotNull Route route,
            @NotBlank @Size(max = 30) String frequency,
            @NotNull @Min(1) @Max(365) Integer durationDays,
            @NotNull @Min(1) @Max(10000) Integer quantity,
            @Size(max = 500) String instructions) {
    }

    public record OrderRequest(
            @NotNull Long patientId,
            @NotNull Long encounterId,
            @NotNull OrderType orderType,
            OrderPriority priority,
            Long labTestId,
            @Size(max = 200) String imagingStudy,
            @Size(max = 100) String bodySite,
            @Size(max = 100) String referralSpecialty,
            @Size(max = 120) String referralTo,
            @Size(max = 500) String clinicalIndication,
            @Size(max = 1000) String instructions,
            @Valid MedicationOrderDetails medication,
            /** true = save as DRAFT (no checks yet); false = place immediately. */
            boolean saveAsDraft,
            /** Required to proceed when decision support raised WARNING/CRITICAL alerts. */
            @Size(max = 500) String overrideReason) {
    }

    public record CdsCheckRequest(
            @NotNull Long patientId,
            @NotNull OrderType orderType,
            Long drugId,
            Long labTestId,
            @Size(max = 200) String imagingStudy) {
    }

    /** One decision-support finding. overridable = false means a hard stop (e.g. contraindicated combination). */
    public record CdsAlert(AlertType type, AlertSeverity severity, String message, boolean overridable) {
    }

    public record CdsResult(List<CdsAlert> alerts, boolean requiresOverride, boolean blocked) {

        public static CdsResult of(List<CdsAlert> alerts) {
            boolean blocked = alerts.stream().anyMatch(a -> !a.overridable());
            boolean requiresOverride = alerts.stream().anyMatch(a -> a.severity() != AlertSeverity.INFO);
            return new CdsResult(alerts, requiresOverride, blocked);
        }
    }

    public record PlaceOrderRequest(@Size(max = 500) String overrideReason) {
    }

    public record StatusRequest(@NotNull OrderStatus status, @Size(max = 500) String note) {
    }

    public record CancelRequest(@NotBlank @Size(max = 500) String reason) {
    }

    /** Numeric value for lab tests; text for qualitative results and imaging reports. */
    public record ResultRequest(
            BigDecimal valueNumeric,
            @Size(max = 4000) String valueText,
            Boolean abnormal,
            ResultStatus resultStatus,
            @Size(max = 1000) String comments) {
    }

    public record OrderDto(
            Long id,
            String orderNo,
            Long patientId,
            String patientName,
            String patientMrn,
            Long encounterId,
            Long orderedById,
            String orderedByName,
            OrderType orderType,
            OrderStatus status,
            OrderPriority priority,
            Long labTestId,
            String labTestName,
            String imagingStudy,
            String bodySite,
            String referralSpecialty,
            String referralTo,
            String clinicalIndication,
            String instructions,
            String alertOverrideReason,
            String summary,
            LocalDateTime placedAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            String cancelReason,
            LocalDateTime createdAt) {
    }

    public record OrderAlertDto(Long id, AlertType alertType, AlertSeverity severity, String message,
                                boolean overridden, LocalDateTime createdAt) {
    }

    public record LabResultDto(
            Long id,
            Long orderId,
            String orderNo,
            OrderType orderType,
            Long patientId,
            Long labTestId,
            String labTestCode,
            String testName,
            BigDecimal valueNumeric,
            String valueText,
            String unit,
            String referenceRange,
            ResultFlag flag,
            ResultStatus resultStatus,
            String comments,
            LocalDateTime resultedAt,
            String resultedByName) {
    }

    public record OrderDetailDto(OrderDto order, List<OrderAlertDto> alerts, LabResultDto result,
                                 PrescriptionDto prescription) {
    }

    public record LabTestDto(Long id, String code, String name, String category, String specimen, String unit,
                             BigDecimal normalLow, BigDecimal normalHigh, BigDecimal criticalLow,
                             BigDecimal criticalHigh, Integer turnaroundHours, boolean active) {
    }

    public record LabTestRequest(
            @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Z0-9_]+$", message = "Use capital letters, digits or _.") String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 60) String category,
            @Size(max = 60) String specimen,
            @Size(max = 30) String unit,
            BigDecimal normalLow,
            BigDecimal normalHigh,
            BigDecimal criticalLow,
            BigDecimal criticalHigh,
            @Min(1) @Max(720) Integer turnaroundHours,
            boolean active) {
    }
}
