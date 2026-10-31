package com.careconnect.common.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Generates human-readable, unique business numbers from PostgreSQL sequences:
 * MRN "CC-2026-000123", order "ORD-000123", prescription "RX-000123".
 * Sequences never hand out the same value twice, even under concurrent requests.
 */
@Component
@RequiredArgsConstructor
public class NumberGenerator {

    private final EntityManager entityManager;

    public String nextMrn() {
        return "CC-" + Year.now().getValue() + "-" + pad(next("mrn_seq"));
    }

    public String nextOrderNo() {
        return "ORD-" + pad(next("order_no_seq"));
    }

    public String nextRxNo() {
        return "RX-" + pad(next("rx_no_seq"));
    }

    private long next(String sequence) {
        // The sequence name is a fixed constant from this class, never user input.
        Object value = entityManager.createNativeQuery("SELECT nextval('" + sequence + "')").getSingleResult();
        return ((Number) value).longValue();
    }

    private static String pad(long value) {
        return String.format("%06d", value);
    }
}
