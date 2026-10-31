package com.careconnect.order.service;

import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.DrugInteraction;
import com.careconnect.medication.model.MedicationEnums.InteractionSeverity;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.DrugInteractionRepository;
import com.careconnect.medication.repository.DrugRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.order.dto.OrderDtos.CdsAlert;
import com.careconnect.order.dto.OrderDtos.CdsResult;
import com.careconnect.order.model.OrderEnums.AlertSeverity;
import com.careconnect.order.model.OrderEnums.AlertType;
import com.careconnect.order.model.OrderEnums.OrderType;
import com.careconnect.order.repository.ClinicalOrderRepository;
import com.careconnect.patient.model.Allergy;
import com.careconnect.patient.model.PatientEnums.AllergySeverity;
import com.careconnect.patient.model.PatientEnums.AllergyType;
import com.careconnect.patient.model.PatientEnums.RecordStatus;
import com.careconnect.patient.repository.AllergyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsServiceTest {

    private static final long PATIENT = 1L;

    @Mock AllergyRepository allergies;
    @Mock PrescriptionRepository prescriptions;
    @Mock DrugInteractionRepository interactions;
    @Mock DrugRepository drugs;
    @Mock ClinicalOrderRepository orders;
    @InjectMocks CdsService cds;

    private Drug amoxicillin;
    private Drug aspirin;
    private Drug warfarin;
    private Drug simvastatin;
    private Drug clarithromycin;

    @BeforeEach
    void setUp() {
        amoxicillin = drug(1L, "Amoxicillin 500 mg", "Amoxicillin", "Penicillins");
        aspirin = drug(2L, "Aspirin 75 mg", "Acetylsalicylic acid", "NSAIDs and antiplatelets");
        warfarin = drug(3L, "Warfarin 5 mg", "Warfarin", "Anticoagulants");
        simvastatin = drug(4L, "Simvastatin 20 mg", "Simvastatin", "Statins");
        clarithromycin = drug(5L, "Clarithromycin 500 mg", "Clarithromycin", "Macrolides");
    }

    @Test
    void penicillinAllergyFlagsAmoxicillinAsCritical() {
        when(drugs.findById(1L)).thenReturn(Optional.of(amoxicillin));
        when(allergies.findByPatientIdAndStatus(PATIENT, RecordStatus.ACTIVE)).thenReturn(List.of(allergy("Penicillin", AllergyType.DRUG)));
        when(prescriptions.findByPatientIdAndStatusIn(eq(PATIENT), any())).thenReturn(List.of());

        List<CdsAlert> alerts = cds.check(PATIENT, OrderType.MEDICATION, 1L, null, null, null);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).type()).isEqualTo(AlertType.DRUG_ALLERGY);
        assertThat(alerts.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alerts.get(0).overridable()).isTrue();
    }

    @Test
    void foodAllergyIsIgnoredForDrugs() {
        when(drugs.findById(1L)).thenReturn(Optional.of(amoxicillin));
        when(allergies.findByPatientIdAndStatus(PATIENT, RecordStatus.ACTIVE)).thenReturn(List.of(allergy("Penicillin", AllergyType.FOOD)));
        when(prescriptions.findByPatientIdAndStatusIn(eq(PATIENT), any())).thenReturn(List.of());

        assertThat(cds.check(PATIENT, OrderType.MEDICATION, 1L, null, null, null)).isEmpty();
    }

    @Test
    void majorInteractionWithCurrentMedicineIsCriticalButOverridable() {
        when(drugs.findById(2L)).thenReturn(Optional.of(aspirin));
        when(allergies.findByPatientIdAndStatus(PATIENT, RecordStatus.ACTIVE)).thenReturn(List.of());
        when(prescriptions.findByPatientIdAndStatusIn(eq(PATIENT), any())).thenReturn(List.of(activeRx(warfarin)));
        when(interactions.findBetween(eq(2L), anyCollection())).thenReturn(List.of(interaction(warfarin, aspirin, InteractionSeverity.MAJOR)));

        List<CdsAlert> alerts = cds.check(PATIENT, OrderType.MEDICATION, 2L, null, null, null);

        assertThat(alerts).singleElement().satisfies(a -> {
            assertThat(a.type()).isEqualTo(AlertType.DRUG_INTERACTION);
            assertThat(a.severity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(a.overridable()).isTrue();
            assertThat(a.message()).contains("Warfarin");
        });
        CdsResult result = CdsResult.of(alerts);
        assertThat(result.requiresOverride()).isTrue();
        assertThat(result.blocked()).isFalse();
    }

    @Test
    void contraindicatedCombinationIsAHardStop() {
        when(drugs.findById(5L)).thenReturn(Optional.of(clarithromycin));
        when(allergies.findByPatientIdAndStatus(PATIENT, RecordStatus.ACTIVE)).thenReturn(List.of());
        when(prescriptions.findByPatientIdAndStatusIn(eq(PATIENT), any())).thenReturn(List.of(activeRx(simvastatin)));
        when(interactions.findBetween(eq(5L), anyCollection()))
                .thenReturn(List.of(interaction(simvastatin, clarithromycin, InteractionSeverity.CONTRAINDICATED)));

        CdsResult result = CdsResult.of(cds.check(PATIENT, OrderType.MEDICATION, 5L, null, null, null));

        assertThat(result.blocked()).isTrue();
    }

    @Test
    void sameDrugAlreadyActiveIsADuplicateWarning() {
        when(drugs.findById(3L)).thenReturn(Optional.of(warfarin));
        when(allergies.findByPatientIdAndStatus(PATIENT, RecordStatus.ACTIVE)).thenReturn(List.of());
        when(prescriptions.findByPatientIdAndStatusIn(eq(PATIENT), any())).thenReturn(List.of(activeRx(warfarin)));

        List<CdsAlert> alerts = cds.check(PATIENT, OrderType.MEDICATION, 3L, null, null, null);

        assertThat(alerts).singleElement().satisfies(a -> {
            assertThat(a.type()).isEqualTo(AlertType.DUPLICATE_ORDER);
            assertThat(a.severity()).isEqualTo(AlertSeverity.WARNING);
        });
    }

    @Test
    void referralsNeedNoChecks() {
        assertThat(cds.check(PATIENT, OrderType.REFERRAL, null, null, null, null)).isEmpty();
    }

    @Test
    void allergyMatchingUsesNameGenericAndClass() {
        assertThat(CdsService.matchesAllergy("Sulfa", drug(9L, "Cotrimoxazole DS", "Sulfamethoxazole + Trimethoprim", "Sulfonamides"))).isTrue();
        assertThat(CdsService.matchesAllergy("Aspirin", aspirin)).isTrue();
        assertThat(CdsService.matchesAllergy("Ibuprofen", aspirin)).isFalse();
        assertThat(CdsService.matchesAllergy("ab", amoxicillin)).isFalse();
    }

    // ----- helpers -----

    private static Drug drug(Long id, String name, String generic, String drugClass) {
        Drug drug = new Drug();
        drug.setId(id);
        drug.setName(name);
        drug.setGenericName(generic);
        drug.setDrugClass(drugClass);
        return drug;
    }

    private static Allergy allergy(String allergen, AllergyType type) {
        Allergy allergy = new Allergy();
        allergy.setAllergen(allergen);
        allergy.setAllergyType(type);
        allergy.setSeverity(AllergySeverity.SEVERE);
        allergy.setStatus(RecordStatus.ACTIVE);
        return allergy;
    }

    private static Prescription activeRx(Drug drug) {
        Prescription rx = new Prescription();
        rx.setId(drug.getId() * 10);
        rx.setRxNo("RX-00000" + drug.getId());
        rx.setDrug(drug);
        rx.setStatus(PrescriptionStatus.VERIFIED);
        rx.setDurationDays(30);
        return rx;
    }

    private static DrugInteraction interaction(Drug a, Drug b, InteractionSeverity severity) {
        DrugInteraction interaction = new DrugInteraction();
        interaction.setDrugA(a);
        interaction.setDrugB(b);
        interaction.setSeverity(severity);
        interaction.setDescription("Test interaction");
        return interaction;
    }

}
