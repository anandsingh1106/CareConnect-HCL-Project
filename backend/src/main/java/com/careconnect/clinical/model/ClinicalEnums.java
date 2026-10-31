package com.careconnect.clinical.model;

public final class ClinicalEnums {

    private ClinicalEnums() {
    }

    public enum EncounterType { OUTPATIENT, INPATIENT, EMERGENCY, TELEHEALTH, FOLLOW_UP }

    public enum EncounterStatus { OPEN, CLOSED, CANCELLED }

    /** PROGRESS = the main SOAP note; ADDENDUM = a later, signed correction/addition to a signed note. */
    public enum NoteType { PROGRESS, ADDENDUM }

    /** DRAFT notes can be edited by their author; SIGNED notes are locked forever. */
    public enum NoteStatus { DRAFT, SIGNED }

    public enum DiagnosisType { PRIMARY, SECONDARY }
}
