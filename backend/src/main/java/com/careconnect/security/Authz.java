package com.careconnect.security;

/**
 * Role rules used in @PreAuthorize, kept in one place so the access matrix is easy to review.
 * "Minimum necessary" principle: each group lists only the roles that need the data for their job.
 */
public final class Authz {

    private Authz() {
    }

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String DOCTOR = "hasRole('DOCTOR')";
    public static final String NURSE = "hasRole('NURSE')";
    public static final String PHARMACIST = "hasRole('PHARMACIST')";
    public static final String PATIENT = "hasRole('PATIENT')";

    /** Doctors and nurses: vitals, allergies, immunizations, MAR, lab result entry. */
    public static final String DOCTOR_OR_NURSE = "hasAnyRole('DOCTOR','NURSE')";

    /** Read the clinical chart (history, notes, orders, medications). */
    public static final String CLINICAL_READ = "hasAnyRole('DOCTOR','NURSE','PHARMACIST')";

    /** Read patient demographics. */
    public static final String DEMOGRAPHICS_READ = "hasAnyRole('DOCTOR','NURSE','PHARMACIST','RECEPTIONIST')";

    /** Search the patient list (admin needs it to link portal accounts; sees summary fields only). */
    public static final String PATIENT_SEARCH = "hasAnyRole('DOCTOR','NURSE','PHARMACIST','RECEPTIONIST','ADMIN')";

    /** Register patients and edit demographics. */
    public static final String REGISTRATION = "hasAnyRole('RECEPTIONIST','DOCTOR')";

    /** Manage slots and appointments. */
    public static final String SCHEDULING = "hasAnyRole('RECEPTIONIST','DOCTOR','NURSE','ADMIN')";

    /** Upload clinical documents. */
    public static final String DOCUMENT_UPLOAD = "hasAnyRole('DOCTOR','NURSE','RECEPTIONIST')";

    /** Drug master and interaction table read access. */
    public static final String DRUG_READ = "hasAnyRole('DOCTOR','NURSE','PHARMACIST','ADMIN')";

    /** Drug master, stock and interaction maintenance. */
    public static final String DRUG_MANAGE = "hasAnyRole('PHARMACIST','ADMIN')";

    /** Code tables (ICD-10, lab tests, note templates) read access. */
    public static final String MASTER_READ = "hasAnyRole('DOCTOR','NURSE','PHARMACIST','ADMIN')";

    /** Secure messaging between patients and doctors. */
    public static final String MESSAGING = "hasAnyRole('DOCTOR','PATIENT')";
}
