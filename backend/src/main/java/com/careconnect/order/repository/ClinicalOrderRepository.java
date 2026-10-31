package com.careconnect.order.repository;

import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.order.model.ClinicalOrder;
import com.careconnect.order.model.OrderEnums.OrderStatus;
import com.careconnect.order.model.OrderEnums.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {

    default ClinicalOrder require(Long id) {
        ClinicalOrder order = findById(id).orElseThrow(() -> ApiException.notFound("Order", id));
        AuditContext.setPatientId(order.getPatient().getId());
        return order;
    }

    @Query("""
            select o from ClinicalOrder o
            where (:status is null or o.status = :status)
              and (:type is null or o.orderType = :type)
              and (:patientId is null or o.patient.id = :patientId)
              and (:orderedById is null or o.orderedBy.id = :orderedById)
            """)
    Page<ClinicalOrder> search(@Param("status") OrderStatus status, @Param("type") OrderType type,
                               @Param("patientId") Long patientId, @Param("orderedById") Long orderedById,
                               Pageable pageable);

    List<ClinicalOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<ClinicalOrder> findByEncounterIdOrderByCreatedAtDesc(Long encounterId);

    /** Duplicate lab check: same test still pending, or resulted in the last 24 hours. */
    @Query("""
            select o from ClinicalOrder o
            where o.patient.id = :patientId and o.labTest.id = :labTestId
              and (o.status in :openStatuses or (o.status = :completed and o.completedAt >= :since))
            """)
    List<ClinicalOrder> findRecentLabOrders(@Param("patientId") Long patientId, @Param("labTestId") Long labTestId,
                                            @Param("openStatuses") Collection<OrderStatus> openStatuses,
                                            @Param("completed") OrderStatus completed,
                                            @Param("since") LocalDateTime since);

    @Query("""
            select o from ClinicalOrder o
            where o.patient.id = :patientId and o.orderType = :type
              and lower(o.imagingStudy) = :study and o.status in :openStatuses
            """)
    List<ClinicalOrder> findOpenOrdersByStudy(@Param("patientId") Long patientId, @Param("type") OrderType type,
                                              @Param("study") String study,
                                              @Param("openStatuses") Collection<OrderStatus> openStatuses);

    long countByOrderedByIdAndStatusIn(Long orderedById, Collection<OrderStatus> statuses);

    long countByOrderedByIdAndStatus(Long orderedById, OrderStatus status);

    long countByStatusInAndOrderTypeIn(Collection<OrderStatus> statuses, Collection<OrderType> types);
}
