package io.github.debajdutta.orderservice.order.service;

import io.github.debajdutta.orderservice.common.exception.BusinessRuleException;
import io.github.debajdutta.orderservice.common.exception.ResourceNotFoundException;
import io.github.debajdutta.orderservice.common.web.CurrentActorProvider;
import io.github.debajdutta.orderservice.order.domain.Order;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.CreateOrderRequest;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.OrderResponse;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.UpdateOrderRequest;
import io.github.debajdutta.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for orders. Controllers stay thin; transactions and rules live here.
 * Phase 2 will add @PreAuthorize on these methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor // constructor injection for final fields
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository repository;
    private final CurrentActorProvider actorProvider;

    public Page<OrderResponse> list(OrderStatus status, Pageable pageable) {
        Page<Order> page = (status == null)
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return page.map(OrderResponse::from);
    }

    public OrderResponse get(Long id) {
        return OrderResponse.from(findOrThrow(id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        Order order = Order.create(req.customerName(), req.product(), req.quantity(), req.unitPrice(),
                actorProvider.currentActor());
        Order saved = repository.save(order);
        log.info("Created order id={} by={}", saved.getId(), saved.getCreatedBy());
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse replace(Long id, UpdateOrderRequest req) {
        Order order = findOrThrow(id);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessRuleException(
                    "Order " + id + " is " + order.getStatus() + "; only CREATED orders can be edited");
        }
        order.setCustomerName(req.customerName());
        order.setProduct(req.product());
        order.setQuantity(req.quantity());
        order.setUnitPrice(req.unitPrice());
        order.setUpdatedBy(actorProvider.currentActor());
        // The entity is managed, so changes would flush at commit anyway; flushing now
        // makes the response carry the incremented version and updatedAt.
        return OrderResponse.from(repository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse changeStatus(Long id, OrderStatus target) {
        Order order = findOrThrow(id);
        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Cannot change order " + id + " from " + current + " to " + target
                            + "; allowed: " + current.allowedNext());
        }
        order.setStatus(target);
        order.setUpdatedBy(actorProvider.currentActor());
        log.info("Order id={} status {} -> {}", id, current, target);
        return OrderResponse.from(repository.saveAndFlush(order));
    }

    @Transactional
    public void delete(Long id) {
        Order order = findOrThrow(id);
        repository.delete(order);
        log.info("Deleted order id={} by={}", id, actorProvider.currentActor());
    }

    private Order findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
