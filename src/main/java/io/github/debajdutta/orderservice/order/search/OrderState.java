package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.order.domain.OrderStatus;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * The API's customer-facing "state", which is coarser than the internal OrderStatus.
 * Not 1:1 - "open" covers two internal statuses - so it has to be mapped both ways:
 *   request:  state=open      -> status IN (CREATED, CONFIRMED)
 *   response: status=CONFIRMED -> "state": "open"
 * Internal statuses can be renamed or split later without breaking API clients.
 */
enum OrderState {

    OPEN("open", OrderStatus.CREATED, OrderStatus.CONFIRMED),
    IN_TRANSIT("in_transit", OrderStatus.SHIPPED),
    DELIVERED("delivered", OrderStatus.DELIVERED),
    CANCELLED("cancelled", OrderStatus.CANCELLED);

    private final String apiValue;
    private final Set<OrderStatus> statuses;

    OrderState(String apiValue, OrderStatus first, OrderStatus... rest) {
        this.apiValue = apiValue;
        this.statuses = EnumSet.of(first, rest);
    }

    public String apiValue() {
        return apiValue;
    }

    public Set<OrderStatus> statuses() {
        return statuses;
    }

    public static Optional<OrderState> fromApiValue(String value) {
        return Arrays.stream(values()).filter(s -> s.apiValue.equalsIgnoreCase(value)).findFirst();
    }

    public static OrderState of(OrderStatus status) {
        return Arrays.stream(values())
                .filter(s -> s.statuses.contains(status))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No API state for status " + status));
    }

    public static String allowedValues() {
        return String.join(", ", Arrays.stream(values()).map(OrderState::apiValue).toList());
    }
}
