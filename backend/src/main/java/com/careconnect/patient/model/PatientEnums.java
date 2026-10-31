package com.careconnect.patient.model;

/** Small enums used by patient records (grouped in one file for readability). */
public final class PatientEnums {

    private PatientEnums() {
    }

    public enum Gender { MALE, FEMALE, OTHER }

    public enum AllergyType { DRUG, FOOD, ENVIRONMENT, OTHER }

    public enum AllergySeverity { MILD, MODERATE, SEVERE }

    public enum RecordStatus { ACTIVE, INACTIVE }

    public enum ProblemStatus { ACTIVE, RESOLVED }

    public enum DocumentCategory { LAB_REPORT, IMAGING, REFERRAL, CONSENT, OTHER }
}
