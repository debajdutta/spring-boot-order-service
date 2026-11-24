package io.github.debajdutta.orderservice.order.repository;

import io.github.debajdutta.orderservice.order.domain.Order;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
