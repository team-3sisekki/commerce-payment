package org.example.commercepayment.domain.order.repository;

import org.example.commercepayment.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}