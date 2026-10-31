package com.careconnect.medication.mapper;

import com.careconnect.medication.dto.MedicationDtos.AdministrationDto;
import com.careconnect.medication.dto.MedicationDtos.DrugDto;
import com.careconnect.medication.dto.MedicationDtos.InteractionDto;
import com.careconnect.medication.dto.MedicationDtos.InventoryTransactionDto;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.DrugInteraction;
import com.careconnect.medication.model.InventoryTransaction;
import com.careconnect.medication.model.MedicationAdministration;
import com.careconnect.medication.model.Prescription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MedicationMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderNo", source = "order.orderNo")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", expression = "java(prescription.getPatient().getFullName())")
    @Mapping(target = "patientMrn", source = "patient.mrn")
    @Mapping(target = "encounterId", source = "encounter.id")
    @Mapping(target = "prescriberId", source = "prescriber.id")
    @Mapping(target = "prescriberName", source = "prescriber.fullName")
    @Mapping(target = "drugId", source = "drug.id")
    @Mapping(target = "drugCode", source = "drug.code")
    @Mapping(target = "drugName", source = "drug.name")
    @Mapping(target = "genericName", source = "drug.genericName")
    @Mapping(target = "drugClass", source = "drug.drugClass")
    @Mapping(target = "drugStock", source = "drug.stockQty")
    @Mapping(target = "currentlyActive", expression = "java(prescription.isCurrentlyActive())")
    @Mapping(target = "verifiedByName", source = "verifiedBy.fullName")
    @Mapping(target = "dispensedByName", source = "dispensedBy.fullName")
    @Mapping(target = "discontinuedByName", source = "discontinuedBy.fullName")
    @Mapping(target = "replacesId", source = "replaces.id")
    PrescriptionDto toDto(Prescription prescription);

    @Mapping(target = "prescriptionId", source = "prescription.id")
    @Mapping(target = "rxNo", source = "prescription.rxNo")
    @Mapping(target = "drugName", source = "prescription.drug.name")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "administeredById", source = "administeredBy.id")
    @Mapping(target = "administeredByName", source = "administeredBy.fullName")
    AdministrationDto toDto(MedicationAdministration administration);

    @Mapping(target = "lowStock", expression = "java(drug.isLowStock())")
    DrugDto toDto(Drug drug);

    @Mapping(target = "drugId", source = "drug.id")
    @Mapping(target = "performedByName", source = "performedBy.fullName")
    InventoryTransactionDto toDto(InventoryTransaction transaction);

    @Mapping(target = "drugAId", source = "drugA.id")
    @Mapping(target = "drugAName", source = "drugA.name")
    @Mapping(target = "drugBId", source = "drugB.id")
    @Mapping(target = "drugBName", source = "drugB.name")
    InteractionDto toDto(DrugInteraction interaction);
}
