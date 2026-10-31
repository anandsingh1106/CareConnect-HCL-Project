package com.careconnect.user.model;

/** The six CareConnect roles. Spring Security sees them as authorities "ROLE_ADMIN", "ROLE_DOCTOR", ... */
public enum Role {
    ADMIN,
    DOCTOR,
    NURSE,
    PHARMACIST,
    RECEPTIONIST,
    PATIENT
}
