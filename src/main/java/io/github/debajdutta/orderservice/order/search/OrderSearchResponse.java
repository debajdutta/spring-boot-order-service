package io.github.debajdutta.orderservice.order.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response shape designed for the API consumer, not copied from the entity:
 * order numbers instead of raw ids, "item" instead of "product", a coarse "state",
 * a calendar date instead of a timestamp, and explicit paging/sort metadata.
 */
public record OrderSearchResponse(
        List<OrderSummary> results,
        Paging paging,
        SortInfo sort) {

    public record OrderSummary(
            String orderNumber,   // "ORD-000042", derived from id
            String customer,
            String item,
            int quantity,
            BigDecimal total,     // quantity * unitPrice
            String state,         // open | in_transit | delivered | cancelled
            LocalDate placedOn,   // date part of createdAt, UTC
            String href) {        // link to the full order: /api/v1/orders/{id}
    }

    public record Paging(int offset, int limit, long totalMatches, boolean hasMore) {
    }

    public record SortInfo(String orderBy, String direction) {
    }
}
