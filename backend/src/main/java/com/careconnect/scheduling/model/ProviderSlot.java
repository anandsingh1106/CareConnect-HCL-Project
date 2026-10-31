package com.careconnect.scheduling.model;

import com.careconnect.common.model.Auditable;
import com.careconnect.scheduling.model.SchedulingEnums.SlotStatus;
import com.careconnect.user.model.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A bookable time slot in a doctor's calendar. The @Version column stops two people booking the same slot. */
@Getter
@Setter
@Entity
@Table(name = "provider_slots")
public class ProviderSlot extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "provider_slots_seq")
    @SequenceGenerator(name = "provider_slots_seq", sequenceName = "provider_slots_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private AppUser provider;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalDateTime slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot_status", nullable = false, length = 10)
    private SlotStatus status = SlotStatus.AVAILABLE;
}
