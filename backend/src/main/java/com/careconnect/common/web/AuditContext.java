package com.careconnect.common.web;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Lets services tell the audit interceptor which patient a request touched
 * (for URLs such as /api/orders/15 that do not contain the patient id).
 */
public final class AuditContext {

    public static final String PATIENT_ID_ATTRIBUTE = "careconnect.audit.patientId";

    private AuditContext() {
    }

    public static void setPatientId(Long patientId) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null && patientId != null) {
            attributes.setAttribute(PATIENT_ID_ATTRIBUTE, patientId, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
