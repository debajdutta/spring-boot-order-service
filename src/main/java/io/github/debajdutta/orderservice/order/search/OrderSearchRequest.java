package io.github.debajdutta.orderservice.order.search;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Exactly what the client sent, in the API's vocabulary - nothing interpreted yet.
 * Everything is optional; defaults and validation are applied by OrderSearchRequestMapper.
 *
 *   API parameter  -> domain meaning
 *   customer       -> Order.customerName (contains, case-insensitive)
 *   item           -> Order.product      (contains, case-insensitive)
 *   state          -> one or more OrderStatus values (open | in_transit | delivered | cancelled)
 *   minTotal/maxTotal -> quantity * unitPrice (computed, not a column)
 *   placedFrom/placedTo -> Order.createdAt (calendar dates, UTC, both inclusive)
 *   orderBy/direction   -> whitelisted sort (placed | total | customer | item, asc | desc)
 *   limit/offset        -> paging
 */
public record OrderSearchRequest(
        String customer,
        String item,
        String state,
        BigDecimal minTotal,
        BigDecimal maxTotal,
        LocalDate placedFrom,
        LocalDate placedTo,
        String orderBy,
        String direction,
        Integer limit,
        Integer offset) {
}
