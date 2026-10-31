package com.careconnect.scheduling.mapper;

import com.careconnect.scheduling.dto.SchedulingDtos.AppointmentDto;
import com.careconnect.scheduling.dto.SchedulingDtos.SlotDto;
import com.careconnect.scheduling.model.Appointment;
import com.careconnect.scheduling.model.ProviderSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SchedulingMapper {

    @Mapping(target = "providerId", source = "provider.id")
    @Mapping(target = "providerName", source = "provider.fullName")
    SlotDto toDto(ProviderSlot slot);

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(appointment.getPatient().getFullName())")
    @Mapping(target = "patientMrn", source = "patient.mrn")
    @Mapping(target = "providerId", source = "provider.id")
    @Mapping(target = "providerName", source = "provider.fullName")
    @Mapping(target = "providerSpecialty", source = "provider.specialty")
    @Mapping(target = "slotId", source = "slot.id")
    AppointmentDto toDto(Appointment appointment);
}
