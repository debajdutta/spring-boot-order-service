package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

/**
 * Internal query model, expressed in DOMAIN terms (entity statuses, instants,
 * sort fields). The API's own vocabulary ("state=open", "placedFrom=2026-11-01",
 * "orderBy=total") is translated into this by OrderSearchRequestMapper.
 * A null filter means "don't filter on this".
 */
record OrderSearchCriteria(
        String customerContains,
        String productContains,
        Set<OrderStatus> statuses,
        BigDecimal minTotal,
        BigDecimal maxTotal,
        Instant createdFromInclusive,
        Instant createdToExclusive,
        OrderSortField sortField,
        boolean descending,
        int offset,
        int limit) {
}
