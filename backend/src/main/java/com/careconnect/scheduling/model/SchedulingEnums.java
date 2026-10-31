package com.careconnect.scheduling.model;

public final class SchedulingEnums {

    private SchedulingEnums() {
    }

    public enum SlotStatus { AVAILABLE, BOOKED, BLOCKED }

    public enum AppointmentType { IN_PERSON, TELEHEALTH }

    public enum AppointmentStatus { BOOKED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW }

    public enum BookedVia { STAFF, PORTAL }
}
