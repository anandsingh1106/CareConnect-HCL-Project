package com.careconnect.order.service;

import com.careconnect.common.exception.ApiException;
import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.DrugInteraction;
import com.careconnect.medication.model.MedicationEnums.InteractionSeverity;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.DrugInteractionRepository;
import com.careconnect.medication.repository.DrugRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.order.dto.OrderDtos.CdsAlert;
import com.careconnect.order.model.ClinicalOrder;
import com.careconnect.order.model.OrderEnums.AlertSeverity;
import com.careconnect.order.model.OrderEnums.AlertType;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.repository.ClinicalOrderRepository;
import com.careconnect.patient.model.Allergy;
import com.careconnect.patient.model.PatientEnums.AllergyType;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import com.careconnect.patient.repository.AllergyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Clinical Decision Support run before an order is placed:
 * 1. Drug-allergy: the drug's name, generic name or class matches an active drug allergy  -> CRITICAL
 * 2. Drug-drug interaction with the patient's current medicines (sample interaction table):
 *    CONTRAINDICATED -> CRITICAL hard stop, MAJOR -> CRITICAL, MODERATE -> WARNING, MINOR -> INFO
 * 3. Duplicate order: same drug already active, same lab test pending or resulted in 24 h,
 *    same imaging study pending -> WARNING
 * WARNING/CRITICAL alerts must be acknowledged with an override reason; hard stops cannot be overridden.
 */
@Service
@RequiredArgsConstructor
public class CdsService {

    private static final EnumSet<OrderStatus> PENDING = EnumSet.of(OrderStatus.DRAFT, OrderStatus.PLACED, OrderStatus.IN_PROGRESS);

    private final AllergyRepository allergies;
    private final PrescriptionRepository prescriptions;
    private final DrugInteractionRepository interactions;
    private final DrugRepository drugs;
    private final ClinicalOrderRepository orders;

    @Transactional(readOnly = true)
    public List<CdsAlert> check(Long patientId, OrderType type, Long drugId, Long labTestId, String imagingStudy,
                                Long excludeOrderId) {
        List<CdsAlert> alerts = switch (type) {
            case MEDICATION -> {
                if (drugId == null) {
                    throw ApiException.badRequest("Select a drug.");
                }
                Drug drug = drugs.findById(drugId).orElseThrow(() -> ApiException.notFound("Drug", drugId));
                yield checkMedication(patientId, drug);
            }
            case LAB -> labTestId == null ? List.of() : checkDuplicateLab(patientId, labTestId, excludeOrderId);
            case IMAGING -> imagingStudy == null || imagingStudy.isBlank() ? List.of()
                    : checkDuplicateImaging(patientId, imagingStudy, excludeOrderId);
            case REFERRAL -> List.of();
        };
        return alerts.stream()
                .sorted(Comparator.comparing(CdsAlert::severity).reversed())
                .toList();
    }

    private List<CdsAlert> checkMedication(Long patientId, Drug drug) {
        List<CdsAlert> alerts = new ArrayList<>();

        // 1. Drug-allergy check
        for (Allergy allergy : allergies.findByPatientIdAndStatus(patientId, RecordStatus.ACTIVE)) {
            if (allergy.getAllergyType() != AllergyType.DRUG && allergy.getAllergyType() != AllergyType.OTHER) {
                continue;
            }
            if (matchesAllergy(allergy.getAllergen(), drug)) {
                alerts.add(new CdsAlert(AlertType.DRUG_ALLERGY, AlertSeverity.CRITICAL,
                        "Allergy: patient is allergic to " + allergy.getAllergen()
                                + " (" + allergy.getSeverity().name().toLowerCase(Locale.ROOT) + ")"
                                + (allergy.getReaction() == null ? "" : ", reaction: " + allergy.getReaction())
                                + ". " + drug.getName() + " (" + nullSafe(drug.getDrugClass()) + ") may cause a reaction.",
                        true));
            }
        }

        // Current medicines = open prescriptions still within their duration
        List<Prescription> current = prescriptions.findByPatientIdAndStatusIn(patientId, PrescriptionStatus.OPEN)
                .stream().filter(Prescription::isCurrentlyActive).toList();

        // 3. Duplicate therapy
        current.stream()
                .filter(p -> p.getDrug().getId().equals(drug.getId()))
                .forEach(p -> alerts.add(new CdsAlert(AlertType.DUPLICATE_ORDER, AlertSeverity.WARNING,
                        "Duplicate: patient already has an active prescription for " + drug.getName()
                                + " (" + p.getRxNo() + ", " + p.getStatus().name().replace('_', ' ').toLowerCase(Locale.ROOT) + ").",
                        true)));

        // 2. Drug-drug interactions (checked in both directions of the interaction table)
        Map<Long, Prescription> otherDrugs = new LinkedHashMap<>();
        current.stream()
                .filter(p -> !p.getDrug().getId().equals(drug.getId()))
                .forEach(p -> otherDrugs.putIfAbsent(p.getDrug().getId(), p));
        if (!otherDrugs.isEmpty()) {
            for (DrugInteraction interaction : interactions.findBetween(drug.getId(), otherDrugs.keySet())) {
                Drug other = interaction.getDrugA().getId().equals(drug.getId()) ? interaction.getDrugB() : interaction.getDrugA();
                Prescription existing = otherDrugs.get(other.getId());
                AlertSeverity severity = switch (interaction.getSeverity()) {
                    case CONTRAINDICATED, MAJOR -> AlertSeverity.CRITICAL;
                    case MODERATE -> AlertSeverity.WARNING;
                    case MINOR -> AlertSeverity.INFO;
                };
                boolean hardStop = interaction.getSeverity() == InteractionSeverity.CONTRAINDICATED;
                alerts.add(new CdsAlert(AlertType.DRUG_INTERACTION, severity,
                        "Interaction (" + interaction.getSeverity().name().toLowerCase(Locale.ROOT) + "): "
                                + drug.getName() + " + " + other.getName() + " (" + existing.getRxNo() + "). "
                                + interaction.getDescription()
                                + (hardStop ? " This combination cannot be prescribed." : ""),
                        !hardStop));
            }
        }
        return alerts;
    }

    private List<CdsAlert> checkDuplicateLab(Long patientId, Long labTestId, Long excludeOrderId) {
        return orders.findRecentLabOrders(patientId, labTestId, PENDING, OrderStatus.COMPLETED,
                        LocalDateTime.now().minusHours(24)).stream()
                .filter(o -> !Objects.equals(o.getId(), excludeOrderId))
                .map(o -> new CdsAlert(AlertType.DUPLICATE_ORDER, AlertSeverity.WARNING,
                        "Duplicate: " + o.getLabTest().getName() + " was already ordered (" + o.getOrderNo() + ", "
                                + describe(o) + ").", true))
                .toList();
    }

    private List<CdsAlert> checkDuplicateImaging(Long patientId, String study, Long excludeOrderId) {
        return orders.findOpenOrdersByStudy(patientId, OrderType.IMAGING, study.trim().toLowerCase(Locale.ROOT), PENDING)
                .stream()
                .filter(o -> !Objects.equals(o.getId(), excludeOrderId))
                .map(o -> new CdsAlert(AlertType.DUPLICATE_ORDER, AlertSeverity.WARNING,
                        "Duplicate: " + o.getImagingStudy() + " is already pending (" + o.getOrderNo() + ").", true))
                .toList();
    }

    /**
     * Text match between an allergen and a drug. Examples that match:
     * "Penicillin" vs class "Penicillins"; "Sulfa" vs class "Sulfonamides"; "Ibuprofen" vs name "Ibuprofen 400 mg".
     */
    static boolean matchesAllergy(String allergen, Drug drug) {
        String a = normalise(allergen);
        if (a.length() < 3) {
            return false;
        }
        for (String field : new String[]{drug.getName(), drug.getGenericName(), drug.getDrugClass()}) {
            if (field == null) {
                continue;
            }
            String f = normalise(field);
            if (f.contains(a) || (f.length() >= 4 && a.contains(f))) {
                return true;
            }
        }
        return false;
    }

    private static String normalise(String value) {
        return value.toLowerCase(Locale.ROOT).replace("allergy", "").replaceAll("\\s+", " ").trim();
    }

    private static String describe(ClinicalOrder order) {
        return order.getStatus() == OrderStatus.COMPLETED ? "resulted in the last 24 hours"
                : order.getStatus().name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String nullSafe(String value) {
        return value == null ? "unclassified" : value;
    }
}
