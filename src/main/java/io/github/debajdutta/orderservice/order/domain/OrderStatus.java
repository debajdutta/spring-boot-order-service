package io.github.debajdutta.orderservice.order.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle. Each status knows which statuses it may move to,
 * so transition rules live with the domain rather than in the controller.
 */
public enum OrderStatus {

    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case CREATED -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return this == target || allowedNext().contains(target);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
