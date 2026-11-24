package io.github.debajdutta.orderservice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for an order.
 *
 * Lombok note: @Getter/@Setter only, deliberately NOT @Data. @Data generates
 * equals/hashCode/toString over all fields, which misbehaves with JPA
 * (lazy loading, changing hash once the id is assigned).
 */
@Entity
@Table(name = "orders") // "order" is a reserved word in SQL
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA, hidden from callers
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 100)
    private String product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** Optimistic locking: concurrent updates to the same order fail instead of silently overwriting. */
    @Version
    private Long version;

    /** Who created / last changed the order. Placeholder until phase 2 fills it from the token's oid. */
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Order create(String customerName, String product, int quantity, BigDecimal unitPrice, String actor) {
        Order order = new Order();
        order.customerName = customerName;
        order.product = product;
        order.quantity = quantity;
        order.unitPrice = unitPrice;
        order.status = OrderStatus.CREATED;
        order.createdBy = actor;
        order.updatedBy = actor;
        return order;
    }

    public BigDecimal getTotalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", status=" + status + "}";
    }
}
