package com.careconnect.medication.model;

import java.util.EnumSet;
import java.util.Set;

public final class MedicationEnums {

    private MedicationEnums() {
    }

    public enum Route { ORAL, IV, IM, SC, TOPICAL, INHALED, SUBLINGUAL, RECTAL, OPHTHALMIC, OTHER }

    /**
     * Prescription workflow:
     * DRAFT -> PENDING_VERIFICATION (order placed) -> VERIFIED (pharmacist) -> DISPENSED (pharmacist).
     * PENDING_VERIFICATION -> REJECTED (pharmacist). Any active state -> DISCONTINUED (doctor).
     */
    public enum PrescriptionStatus {
        DRAFT, PENDING_VERIFICATION, VERIFIED, DISPENSED, REJECTED, DISCONTINUED;

        public static final Set<PrescriptionStatus> OPEN = EnumSet.of(PENDING_VERIFICATION, VERIFIED, DISPENSED);
    }

    public enum AdministrationStatus { GIVEN, HELD, REFUSED, MISSED }

    public enum InventoryTxnType { RECEIPT, DISPENSE, ADJUSTMENT }

    public enum InteractionSeverity { MINOR, MODERATE, MAJOR, CONTRAINDICATED }
}
