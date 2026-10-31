package com.careconnect.order.service;

import com.careconnect.clinical.model.ClinicalEnums.EncounterStatus;
import com.careconnect.clinical.model.Encounter;
import com.careconnect.clinical.repository.EncounterRepository;
import com.careconnect.common.dto.PageResponse;
import com.careconnect.common.exception.ApiException;
import com.careconnect.common.service.NumberGenerator;
import com.careconnect.common.web.AuditContext;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.DrugRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.notification.service.NotificationService;
import com.careconnect.order.dto.OrderDtos.CdsAlert;
import com.careconnect.order.dto.OrderDtos.CdsCheckRequest;
import com.careconnect.order.dto.OrderDtos.CdsResult;
import com.careconnect.order.dto.OrderDtos.LabResultDto;
import com.careconnect.order.dto.OrderDtos.MedicationOrderDetails;
import com.careconnect.order.dto.OrderDtos.OrderDetailDto;
import com.careconnect.order.dto.OrderDtos.OrderDto;
import com.careconnect.order.dto.OrderDtos.OrderRequest;
import com.careconnect.order.dto.OrderDtos.ResultRequest;
import com.careconnect.order.mapper.OrderMapper;
import com.careconnect.order.model.ClinicalOrder;
import com.careconnect.order.model.LabResult;
import com.careconnect.order.model.LabTest;
import com.careconnect.order.model.OrderAlert;
import com.careconnect.order.model.OrderEnums.AlertSeverity;
import com.careconnect.order.model.OrderEnums.OrderPriority;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.model.OrderEnums.ResultFlag;
import com.careconnect.order.model.OrderEnums.ResultStatus;
import com.careconnect.order.repository.ClinicalOrderRepository;
import com.careconnect.order.repository.LabResultRepository;
import com.careconnect.order.repository.LabTestRepository;
import com.careconnect.order.repository.OrderAlertRepository;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.SecurityUtils;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computerized Provider Order Entry.
 * Placing an order (directly or from DRAFT) always runs decision support first; the order is saved only
 * if there are no alerts, or the prescriber supplied an override reason for overridable alerts.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ClinicalOrderRepository orders;
    private final OrderAlertRepository alerts;
    private final LabResultRepository results;
    private final LabTestRepository labTests;
    private final PrescriptionRepository prescriptions;
    private final DrugRepository drugs;
    private final PatientRepository patients;
    private final EncounterRepository encounters;
    private final AppUserRepository users;
    private final CdsService cdsService;
    private final NotificationService notifications;
    private final NumberGenerator numberGenerator;
    private final OrderMapper mapper;
    private final MedicationMapper medicationMapper;

    // ------------------------------------------------------------------ decision support

    @Transactional(readOnly = true)
    public CdsResult check(CdsCheckRequest request) {
        patients.require(request.patientId());
        return CdsResult.of(cdsService.check(request.patientId(), request.orderType(), request.drugId(),
                request.labTestId(), request.imagingStudy(), null));
    }

    // ------------------------------------------------------------------ create / place

    @Transactional
    public OrderDetailDto create(OrderRequest request) {
        return detail(createOrder(request, null, true).getId());
    }

    /** Used when a doctor modifies a prescription: the replacement is a new medication order. */
    @Transactional
    public OrderDetailDto createReplacement(OrderRequest request, Prescription replaces) {
        return detail(createOrder(request, replaces, false).getId());
    }

    private ClinicalOrder createOrder(OrderRequest request, Prescription replaces, boolean requireOpenEncounter) {
        Patient patient = patients.require(request.patientId());
        Encounter encounter = encounters.findById(request.encounterId())
                .orElseThrow(() -> ApiException.notFound("Encounter", request.encounterId()));
        if (!encounter.getPatient().getId().equals(patient.getId())) {
            throw ApiException.badRequest("The encounter belongs to a different patient.");
        }
        if (requireOpenEncounter && encounter.getStatus() != EncounterStatus.OPEN) {
            throw ApiException.conflict("ENCOUNTER_CLOSED", "Orders can only be placed on an open encounter.");
        }

        ClinicalOrder order = new ClinicalOrder();
        order.setOrderNo(numberGenerator.nextOrderNo());
        order.setPatient(patient);
        order.setEncounter(encounter);
        order.setOrderedBy(users.require(SecurityUtils.currentUser().id()));
        order.setOrderType(request.orderType());
        order.setPriority(request.priority() == null ? OrderPriority.ROUTINE : request.priority());
        order.setClinicalIndication(request.clinicalIndication());
        order.setInstructions(request.instructions());
        order.setStatus(OrderStatus.DRAFT);

        Drug drug = applyTypeSpecificFields(order, request);

        List<CdsAlert> found = List.of();
        if (!request.saveAsDraft()) {
            found = cdsService.check(patient.getId(), order.getOrderType(), drug == null ? null : drug.getId(),
                    order.getLabTest() == null ? null : order.getLabTest().getId(), order.getImagingStudy(), null);
            enforceAlerts(found, request.overrideReason());
            markPlaced(order, found, request.overrideReason());
        }
        orders.save(order);
        saveAlerts(order, found);

        if (order.getOrderType() == OrderType.MEDICATION) {
            Prescription prescription = buildPrescription(order, drug, request.medication(), replaces);
            prescriptions.save(prescription);
        }
        if (order.getStatus() == OrderStatus.PLACED) {
            notifyPlaced(order);
        }
        return order;
    }

    @Transactional
    public OrderDetailDto place(Long orderId, String overrideReason) {
        ClinicalOrder order = orders.require(orderId);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw ApiException.conflict("INVALID_STATUS", "Only draft orders can be placed.");
        }
        if (order.getEncounter().getStatus() != EncounterStatus.OPEN) {
            throw ApiException.conflict("ENCOUNTER_CLOSED", "Orders can only be placed on an open encounter.");
        }
        Prescription prescription = prescriptions.findByOrderId(orderId).orElse(null);
        List<CdsAlert> found = cdsService.check(order.getPatient().getId(), order.getOrderType(),
                prescription == null ? null : prescription.getDrug().getId(),
                order.getLabTest() == null ? null : order.getLabTest().getId(), order.getImagingStudy(), orderId);
        enforceAlerts(found, overrideReason);
        markPlaced(order, found, overrideReason);
        saveAlerts(order, found);
        if (prescription != null) {
            prescription.setStatus(PrescriptionStatus.PENDING_VERIFICATION);
        }
        notifyPlaced(order);
        return detail(orderId);
    }

    /**
     * Stops the request with 409 when alerts need attention. The response "details" carries the alerts so
     * the UI can show them and ask for an override reason.
     */
    private void enforceAlerts(List<CdsAlert> found, String overrideReason) {
        CdsResult result = CdsResult.of(found);
        if (result.blocked()) {
            throw new ApiException(HttpStatus.CONFLICT, "CDS_BLOCKED",
                    "This order cannot be placed because of a contraindication.", result);
        }
        if (result.requiresOverride() && (overrideReason == null || overrideReason.isBlank())) {
            throw new ApiException(HttpStatus.CONFLICT, "CDS_ALERTS",
                    "Clinical decision support raised alerts. Review them and give an override reason to proceed.", result);
        }
    }

    private void markPlaced(ClinicalOrder order, List<CdsAlert> found, String overrideReason) {
        order.setStatus(OrderStatus.PLACED);
        order.setPlacedAt(LocalDateTime.now());
        boolean overridden = found.stream().anyMatch(a -> a.severity() != AlertSeverity.INFO);
        order.setAlertOverrideReason(overridden ? overrideReason.trim() : null);
    }

    private void saveAlerts(ClinicalOrder order, List<CdsAlert> found) {
        for (CdsAlert cds : found) {
            OrderAlert alert = new OrderAlert();
            alert.setOrder(order);
            alert.setAlertType(cds.type());
            alert.setSeverity(cds.severity());
            alert.setMessage(cds.message().length() > 500 ? cds.message().substring(0, 500) : cds.message());
            alert.setOverridden(cds.severity() != AlertSeverity.INFO);
            alert.setCreatedAt(LocalDateTime.now());
            alerts.save(alert);
        }
    }

    private Drug applyTypeSpecificFields(ClinicalOrder order, OrderRequest request) {
        switch (request.orderType()) {
            case LAB -> {
                if (request.labTestId() == null) {
                    throw ApiException.badRequest("Select a lab test.");
                }
                LabTest test = labTests.findById(request.labTestId())
                        .orElseThrow(() -> ApiException.notFound("Lab test", request.labTestId()));
                if (!test.isActive()) {
                    throw ApiException.badRequest(test.getName() + " is no longer offered.");
                }
                order.setLabTest(test);
            }
            case IMAGING -> {
                if (request.imagingStudy() == null || request.imagingStudy().isBlank()) {
                    throw ApiException.badRequest("Enter the imaging study, e.g. Chest X-ray PA view.");
                }
                order.setImagingStudy(request.imagingStudy().trim());
                order.setBodySite(request.bodySite());
            }
            case REFERRAL -> {
                if (request.referralSpecialty() == null || request.referralSpecialty().isBlank()) {
                    throw ApiException.badRequest("Enter the specialty to refer to.");
                }
                order.setReferralSpecialty(request.referralSpecialty().trim());
                order.setReferralTo(request.referralTo());
            }
            case MEDICATION -> {
                MedicationOrderDetails med = request.medication();
                if (med == null) {
                    throw ApiException.badRequest("Enter the medication details.");
                }
                Drug drug = drugs.findById(med.drugId()).orElseThrow(() -> ApiException.notFound("Drug", med.drugId()));
                if (!drug.isActive()) {
                    throw ApiException.badRequest(drug.getName() + " is not available for prescribing.");
                }
                return drug;
            }
        }
        return null;
    }

    private Prescription buildPrescription(ClinicalOrder order, Drug drug, MedicationOrderDetails med, Prescription replaces) {
        Prescription prescription = new Prescription();
        prescription.setRxNo(numberGenerator.nextRxNo());
        prescription.setOrder(order);
        prescription.setPatient(order.getPatient());
        prescription.setEncounter(order.getEncounter());
        prescription.setPrescriber(order.getOrderedBy());
        prescription.setDrug(drug);
        prescription.setDose(med.dose().trim());
        prescription.setRoute(med.route());
        prescription.setFrequency(med.frequency().trim());
        prescription.setDurationDays(med.durationDays());
        prescription.setQuantity(med.quantity());
        prescription.setInstructions(med.instructions());
        prescription.setReplaces(replaces);
        prescription.setStatus(order.getStatus() == OrderStatus.DRAFT
                ? PrescriptionStatus.DRAFT : PrescriptionStatus.PENDING_VERIFICATION);
        return prescription;
    }

    private void notifyPlaced(ClinicalOrder order) {
        String patient = order.getPatient().getFullName() + " (" + order.getPatient().getMrn() + ")";
        if (order.getOrderType() == OrderType.MEDICATION) {
            notifications.notifyRole(Role.PHARMACIST, "Prescription awaiting verification",
                    "New medication order " + order.getOrderNo() + " for " + patient + ".",
                    NotificationService.TYPE_PRESCRIPTION, "/pharmacy");
        } else if (order.getPriority() != OrderPriority.ROUTINE) {
            notifications.notifyRole(Role.NURSE, order.getPriority() + " order placed",
                    summaryOf(order, null) + " for " + patient + ".", NotificationService.TYPE_ORDER, "/orders");
        }
    }

    // ------------------------------------------------------------------ lifecycle

    /** Manual progress updates for LAB, IMAGING and REFERRAL orders. Medication orders follow the pharmacy workflow. */
    @Transactional
    public OrderDetailDto updateStatus(Long orderId, OrderStatus next) {
        ClinicalOrder order = orders.require(orderId);
        if (order.getOrderType() == OrderType.MEDICATION) {
            throw ApiException.conflict("PHARMACY_WORKFLOW", "Medication orders are progressed by pharmacy verification and dispensing.");
        }
        if (next == OrderStatus.CANCELLED) {
            throw ApiException.badRequest("Use the cancel action and give a reason.");
        }
        if (next == OrderStatus.COMPLETED && order.getOrderType() != OrderType.REFERRAL) {
            throw ApiException.badRequest("Enter the result to complete a lab or imaging order.");
        }
        transition(order, next);
        return detail(orderId);
    }

    @Transactional
    public OrderDetailDto cancel(Long orderId, String reason) {
        ClinicalOrder order = orders.require(orderId);
        Prescription prescription = prescriptions.findByOrderId(orderId).orElse(null);
        if (prescription != null && prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw ApiException.conflict("ALREADY_DISPENSED", "This medicine was already dispensed. Discontinue the prescription instead.");
        }
        transition(order, OrderStatus.CANCELLED);
        order.setCancelReason(reason.trim());
        if (prescription != null && prescription.getStatus() != PrescriptionStatus.REJECTED) {
            prescription.setStatus(PrescriptionStatus.DISCONTINUED);
            prescription.setDiscontinuedAt(LocalDateTime.now());
            prescription.setDiscontinuedBy(users.require(SecurityUtils.currentUser().id()));
            prescription.setDiscontinueReason("Order cancelled: " + reason.trim());
        }
        return detail(orderId);
    }

    /** Applies a lifecycle transition and stamps the matching timestamp. Package-visible for the pharmacy workflow. */
    public void transition(ClinicalOrder order, OrderStatus next) {
        if (!order.getStatus().canMoveTo(next)) {
            throw ApiException.conflict("INVALID_STATUS",
                    "An order that is " + order.getStatus() + " cannot become " + next + ".");
        }
        LocalDateTime now = LocalDateTime.now();
        switch (next) {
            case PLACED -> order.setPlacedAt(now);
            case IN_PROGRESS -> order.setStartedAt(now);
            case COMPLETED -> {
                if (order.getStartedAt() == null) {
                    order.setStartedAt(now);
                }
                order.setCompletedAt(now);
            }
            case CANCELLED -> order.setCancelledAt(now);
            default -> {
            }
        }
        order.setStatus(next);
    }

    // ------------------------------------------------------------------ results

    /**
     * Records a lab value (flagged against the test's ranges) or an imaging report, and completes the order.
     * A completed result can later be replaced only with status CORRECTED.
     */
    @Transactional
    public LabResultDto enterResult(Long orderId, ResultRequest request) {
        ClinicalOrder order = orders.require(orderId);
        if (order.getOrderType() != OrderType.LAB && order.getOrderType() != OrderType.IMAGING) {
            throw ApiException.badRequest("Results can only be entered for lab and imaging orders.");
        }
        if (request.valueNumeric() == null && (request.valueText() == null || request.valueText().isBlank())) {
            throw ApiException.badRequest("Enter a numeric value or a text result.");
        }
        LabResult result = results.findByOrderId(orderId).orElse(null);
        ResultStatus status = request.resultStatus() == null ? ResultStatus.FINAL : request.resultStatus();
        if (result != null && status != ResultStatus.CORRECTED) {
            throw ApiException.conflict("RESULT_EXISTS", "A result already exists. Submit it with status CORRECTED to amend it.");
        }
        if (result == null) {
            if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.IN_PROGRESS) {
                throw ApiException.conflict("INVALID_STATUS", "Results can only be entered for placed or in-progress orders.");
            }
            result = new LabResult();
            result.setOrder(order);
            result.setLabTest(order.getLabTest());
        }

        LabTest test = order.getLabTest();
        result.setValueNumeric(request.valueNumeric());
        result.setValueText(request.valueText());
        result.setUnit(test == null ? null : test.getUnit());
        result.setReferenceRange(test == null ? null : rangeText(test));
        result.setFlag(flag(test, request.valueNumeric(), request.abnormal()));
        result.setResultStatus(status);
        result.setComments(request.comments());
        result.setResultedAt(LocalDateTime.now());
        result.setResultedBy(users.require(SecurityUtils.currentUser().id()));
        results.save(result);

        if (order.getStatus() != OrderStatus.COMPLETED) {
            transition(order, OrderStatus.COMPLETED);
        }
        notifyResult(order, result);
        return mapper.toDto(result);
    }

    /** Critical limits are checked first, then the normal range. Text-only results use the "abnormal" tick box. */
    public static ResultFlag flag(LabTest test, BigDecimal value, Boolean abnormal) {
        if (value == null || test == null) {
            return Boolean.TRUE.equals(abnormal) ? ResultFlag.ABNORMAL : ResultFlag.NORMAL;
        }
        if (test.getCriticalLow() != null && value.compareTo(test.getCriticalLow()) < 0) {
            return ResultFlag.CRITICAL_LOW;
        }
        if (test.getCriticalHigh() != null && value.compareTo(test.getCriticalHigh()) > 0) {
            return ResultFlag.CRITICAL_HIGH;
        }
        if (test.getNormalLow() != null && value.compareTo(test.getNormalLow()) < 0) {
            return ResultFlag.LOW;
        }
        if (test.getNormalHigh() != null && value.compareTo(test.getNormalHigh()) > 0) {
            return ResultFlag.HIGH;
        }
        return ResultFlag.NORMAL;
    }

    private static String rangeText(LabTest test) {
        if (test.getNormalLow() == null && test.getNormalHigh() == null) {
            return null;
        }
        return plain(test.getNormalLow()) + " - " + plain(test.getNormalHigh());
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private void notifyResult(ClinicalOrder order, LabResult result) {
        boolean abnormal = result.getFlag() != ResultFlag.NORMAL;
        String value = result.getValueNumeric() != null
                ? plain(result.getValueNumeric()) + (result.getUnit() == null ? "" : " " + result.getUnit())
                : "report available";
        notifications.notifyUser(order.getOrderedBy().getId(),
                abnormal ? (result.getFlag().name().startsWith("CRITICAL") ? "CRITICAL lab result" : "Abnormal result") : "Result available",
                summaryOf(order, null) + ": " + value + " (" + result.getFlag() + ") for "
                        + order.getPatient().getFullName() + " (" + order.getPatient().getMrn() + ").",
                NotificationService.TYPE_LAB_RESULT, "/patients/" + order.getPatient().getId());
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> search(OrderStatus status, OrderType type, Long patientId, boolean mine, Pageable pageable) {
        Long orderedBy = mine ? SecurityUtils.currentUser().id() : null;
        Page<ClinicalOrder> page = orders.search(status, type, patientId, orderedBy, pageable);
        Map<Long, Prescription> rx = prescriptionsFor(page.getContent());
        return PageResponse.of(page.map(o -> mapper.toDto(o, summaryOf(o, rx.get(o.getId())))));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> forPatient(Long patientId) {
        patients.require(patientId);
        return toDtos(orders.findByPatientIdOrderByCreatedAtDesc(patientId));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> forEncounter(Long encounterId) {
        encounters.require(encounterId);
        return toDtos(orders.findByEncounterIdOrderByCreatedAtDesc(encounterId));
    }

    @Transactional(readOnly = true)
    public OrderDetailDto detail(Long orderId) {
        ClinicalOrder order = orders.require(orderId);
        AuditContext.setPatientId(order.getPatient().getId());
        Prescription prescription = prescriptions.findByOrderId(orderId).orElse(null);
        return new OrderDetailDto(
                mapper.toDto(order, summaryOf(order, prescription)),
                alerts.findByOrderIdOrderByIdAsc(orderId).stream().map(mapper::toDto).toList(),
                results.findByOrderId(orderId).map(mapper::toDto).orElse(null),
                prescription == null ? null : medicationMapper.toDto(prescription));
    }

    @Transactional(readOnly = true)
    public List<LabResultDto> resultsForPatient(Long patientId) {
        patients.require(patientId);
        return results.findForPatient(patientId).stream().map(mapper::toDto).toList();
    }

    private List<OrderDto> toDtos(List<ClinicalOrder> list) {
        Map<Long, Prescription> rx = prescriptionsFor(list);
        return list.stream().map(o -> mapper.toDto(o, summaryOf(o, rx.get(o.getId())))).toList();
    }

    private Map<Long, Prescription> prescriptionsFor(List<ClinicalOrder> list) {
        List<Long> medicationOrderIds = list.stream()
                .filter(o -> o.getOrderType() == OrderType.MEDICATION).map(ClinicalOrder::getId).toList();
        if (medicationOrderIds.isEmpty()) {
            return Map.of();
        }
        return prescriptions.findByOrderIdIn(medicationOrderIds).stream()
                .collect(Collectors.toMap(p -> p.getOrder().getId(), Function.identity()));
    }

    /** One-line human description, e.g. "HbA1c", "Chest X-ray PA view", "Metformin 500 mg - 500 mg ORAL BD". */
    static String summaryOf(ClinicalOrder order, Prescription prescription) {
        return switch (order.getOrderType()) {
            case LAB -> order.getLabTest() == null ? "Lab test" : order.getLabTest().getName();
            case IMAGING -> order.getImagingStudy() + (order.getBodySite() == null ? "" : " (" + order.getBodySite() + ")");
            case REFERRAL -> "Referral to " + order.getReferralSpecialty()
                    + (order.getReferralTo() == null ? "" : " - " + order.getReferralTo());
            case MEDICATION -> prescription == null ? "Medication"
                    : prescription.getDrug().getName() + " - " + prescription.getDose() + " "
                    + prescription.getRoute() + " " + prescription.getFrequency();
        };
    }
}
