package com.careconnect.order.repository;

import com.careconnect.order.model.OrderAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderAlertRepository extends JpaRepository<OrderAlert, Long> {

    List<OrderAlert> findByOrderIdOrderByIdAsc(Long orderId);
}
