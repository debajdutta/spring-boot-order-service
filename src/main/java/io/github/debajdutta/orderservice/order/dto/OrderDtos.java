package io.github.debajdutta.orderservice.order.dto;

import io.github.debajdutta.orderservice.order.domain.Order;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request/response DTOs as Java records: immutable, no Lombok needed.
 * The entity is never exposed directly over HTTP.
 */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** POST /api/orders */
    public record CreateOrderRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Size(max = 100) String product,
            @NotNull @Min(1) @Max(10_000) Integer quantity,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {
    }

    /** PUT /api/orders/{id} - full replacement of the editable fields. */
    public record UpdateOrderRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Size(max = 100) String product,
            @NotNull @Min(1) @Max(10_000) Integer quantity,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {
    }

    /** PATCH /api/orders/{id}/status */
    public record UpdateStatusRequest(@NotNull OrderStatus status) {
    }

    public record OrderResponse(
            Long id,
            String customerName,
            String product,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            OrderStatus status,
            Long version,
            String createdBy,
            String updatedBy,
            Instant createdAt,
            Instant updatedAt) {

        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(), o.getCustomerName(), o.getProduct(), o.getQuantity(),
                    o.getUnitPrice(), o.getTotalAmount(), o.getStatus(), o.getVersion(),
                    o.getCreatedBy(), o.getUpdatedBy(), o.getCreatedAt(), o.getUpdatedAt());
        }
    }
}
