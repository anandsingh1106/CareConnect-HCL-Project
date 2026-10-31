package com.careconnect.medication.service;

import com.careconnect.common.exception.ApiException;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.medication.mapper.MedicationMapper;
import com.careconnect.medication.model.Drug;
import com.careconnect.medication.model.InventoryTransaction;
import com.careconnect.medication.model.MedicationEnums.InventoryTxnType;
import com.careconnect.medication.model.MedicationEnums.PrescriptionStatus;
import com.careconnect.medication.model.Prescription;
import com.careconnect.medication.repository.InventoryTransactionRepository;
import com.careconnect.medication.repository.PrescriptionRepository;
import com.careconnect.notification.service.NotificationService;
import com.careconnect.order.mapper.OrderMapper;
import com.careconnect.order.model.ClinicalOrder;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.repository.OrderAlertRepository;
import com.careconnect.order.service.OrderService;
import com.careconnect.patient.repository.AllergyRepository;
import com.careconnect.patient.repository.PatientRepository;
import com.careconnect.security.UserPrincipal;
import com.careconnect.user.model.AppUser;
import com.careconnect.user.model.Role;
import com.careconnect.user.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock PrescriptionRepository prescriptions;
    @Mock InventoryTransactionRepository inventory;
    @Mock OrderAlertRepository alerts;
    @Mock AllergyRepository allergies;
    @Mock PatientRepository patients;
    @Mock AppUserRepository users;
    @Mock OrderService orderService;
    @Mock NotificationService notifications;
    @Mock MedicationMapper mapper;
    @Mock OrderMapper orderMapper;
    @InjectMocks PrescriptionService service;

    private Prescription rx;
    private Drug drug;

    @BeforeEach
    void setUp() {
        UserPrincipal pharmacist = new UserPrincipal(5L, "pharm.arjun", "Arjun Singh", Role.PHARMACIST, null, null, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pharmacist, null, pharmacist.getAuthorities()));

        drug = new Drug();
        drug.setId(11L);
        drug.setName("Warfarin 5 mg");
        drug.setStockQty(90);
        drug.setReorderLevel(30);

        rx = new Prescription();
        rx.setId(3L);
        rx.setRxNo("RX-000003");
        rx.setDrug(drug);
        rx.setQuantity(30);
        rx.setOrder(new ClinicalOrder());
        rx.setStatus(PrescriptionStatus.VERIFIED);

        when(prescriptions.require(3L)).thenReturn(rx);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dispenseReducesStockWritesLedgerAndCompletesOrder() {
        when(users.require(5L)).thenReturn(new AppUser());
        when(mapper.toDto(rx)).thenReturn(null);

        PrescriptionDto ignored = service.dispense(3L, null);

        assertThat(drug.getStockQty()).isEqualTo(60);
        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.DISPENSED);
        assertThat(rx.getDispensedQty()).isEqualTo(30);
        ArgumentCaptor<InventoryTransaction> txn = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventory).save(txn.capture());
        assertThat(txn.getValue().getChangeQty()).isEqualTo(-30);
        assertThat(txn.getValue().getBalanceAfter()).isEqualTo(60);
        assertThat(txn.getValue().getTxnType()).isEqualTo(InventoryTxnType.DISPENSE);
        verify(orderService).transition(rx.getOrder(), OrderStatus.COMPLETED);
        verify(notifications, never()).notifyRole(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void dispensingBelowReorderLevelNotifiesPharmacists() {
        drug.setStockQty(40);
        when(users.require(5L)).thenReturn(new AppUser());

        service.dispense(3L, null);

        assertThat(drug.getStockQty()).isEqualTo(10);
        verify(notifications).notifyRole(eq(Role.PHARMACIST), anyString(), anyString(), eq(NotificationService.TYPE_LOW_STOCK), anyString());
    }

    @Test
    void insufficientStockIsRejected() {
        drug.setStockQty(10);

        assertThatThrownBy(() -> service.dispense(3L, null))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "INSUFFICIENT_STOCK");
        assertThat(drug.getStockQty()).isEqualTo(10);
        verify(inventory, never()).save(any());
    }

    @Test
    void onlyVerifiedPrescriptionsCanBeDispensed() {
        rx.setStatus(PrescriptionStatus.PENDING_VERIFICATION);

        assertThatThrownBy(() -> service.dispense(3L, null))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_STATUS");
    }

    @Test
    void verifyMovesOrderInProgress() {
        rx.setStatus(PrescriptionStatus.PENDING_VERIFICATION);
        when(users.require(5L)).thenReturn(new AppUser());

        service.verify(3L, "Dose checked");

        assertThat(rx.getStatus()).isEqualTo(PrescriptionStatus.VERIFIED);
        assertThat(rx.getPharmacistNote()).isEqualTo("Dose checked");
        verify(orderService).transition(rx.getOrder(), OrderStatus.IN_PROGRESS);
    }
}
