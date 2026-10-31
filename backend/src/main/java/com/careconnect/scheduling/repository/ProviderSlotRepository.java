package com.careconnect.scheduling.repository;

import com.careconnect.scheduling.model.ProviderSlot;
import com.careconnect.scheduling.model.SchedulingEnums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProviderSlotRepository extends JpaRepository<ProviderSlot, Long> {

    List<ProviderSlot> findByProviderIdAndSlotStartBetweenOrderBySlotStartAsc(Long providerId, LocalDateTime from, LocalDateTime to);

    List<ProviderSlot> findByProviderIdAndStatusAndSlotStartBetweenOrderBySlotStartAsc(Long providerId, SlotStatus status,
                                                                                         LocalDateTime from, LocalDateTime to);

    boolean existsByProviderIdAndSlotStart(Long providerId, LocalDateTime slotStart);

    boolean existsBySlotStartAfter(LocalDateTime time);

    long countByStatusAndSlotStartBetween(SlotStatus status, LocalDateTime from, LocalDateTime to);
}
