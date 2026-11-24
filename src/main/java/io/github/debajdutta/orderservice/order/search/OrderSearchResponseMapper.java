package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.order.domain.Order;
import io.github.debajdutta.orderservice.order.search.OrderSearchResponse.OrderSummary;
import io.github.debajdutta.orderservice.order.search.OrderSearchResponse.Paging;
import io.github.debajdutta.orderservice.order.search.OrderSearchResponse.SortInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** Maps entities + query metadata (domain vocabulary) back into the API response (client vocabulary). */
@Component
class OrderSearchResponseMapper {

    public OrderSearchResponse toResponse(OrderSearchResult result, OrderSearchCriteria criteria) {
        List<OrderSummary> summaries = result.orders().stream()
                .map(this::toSummary)
                .toList();

        boolean hasMore = (long) criteria.offset() + summaries.size() < result.totalMatches();
        Paging paging = new Paging(criteria.offset(), criteria.limit(), result.totalMatches(), hasMore);

        SortInfo sort = new SortInfo(
                OrderSortOption.of(criteria.sortField()).apiName(),
                criteria.descending() ? "desc" : "asc");

        return new OrderSearchResponse(summaries, paging, sort);
    }

    private OrderSummary toSummary(Order o) {
        return new OrderSummary(
                "ORD-%06d".formatted(o.getId()),
                o.getCustomerName(),
                o.getProduct(),
                o.getQuantity(),
                o.getTotalAmount(),
                OrderState.of(o.getStatus()).apiValue(),
                LocalDate.ofInstant(o.getCreatedAt(), ZoneOffset.UTC),
                "/api/v1/orders/" + o.getId());
    }
}
