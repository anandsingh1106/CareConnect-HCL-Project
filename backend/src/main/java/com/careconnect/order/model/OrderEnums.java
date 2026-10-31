package com.careconnect.order.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderEnums {

    private OrderEnums() {
    }

    public enum OrderType { LAB, IMAGING, MEDICATION, REFERRAL }

    public enum OrderPriority { ROUTINE, URGENT, STAT }

    /**
     * Order lifecycle: DRAFT -> PLACED -> IN_PROGRESS -> COMPLETED, and any non-final state -> CANCELLED.
     * COMPLETED and CANCELLED are final.
     */
    public enum OrderStatus {
        DRAFT, PLACED, IN_PROGRESS, COMPLETED, CANCELLED;

        private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
                DRAFT, EnumSet.of(PLACED, CANCELLED),
                PLACED, EnumSet.of(IN_PROGRESS, COMPLETED, CANCELLED),
                IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED),
                COMPLETED, EnumSet.noneOf(OrderStatus.class),
                CANCELLED, EnumSet.noneOf(OrderStatus.class));

        public boolean canMoveTo(OrderStatus next) {
            return ALLOWED.get(this).contains(next);
        }

        public boolean isFinal() {
            return this == COMPLETED || this == CANCELLED;
        }
    }

    public enum AlertType { DRUG_ALLERGY, DRUG_INTERACTION, DUPLICATE_ORDER }

    /** INFO is shown only; WARNING and CRITICAL need an override reason to proceed. */
    public enum AlertSeverity { INFO, WARNING, CRITICAL }

    public enum ResultFlag { NORMAL, LOW, HIGH, CRITICAL_LOW, CRITICAL_HIGH, ABNORMAL }

    public enum ResultStatus { PRELIMINARY, FINAL, CORRECTED }
}
