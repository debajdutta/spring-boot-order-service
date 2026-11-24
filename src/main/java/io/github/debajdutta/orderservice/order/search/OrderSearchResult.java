package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.order.domain.Order;

import java.util.List;

/** One page of matching orders plus the total number of matches (for paging info). */
record OrderSearchResult(List<Order> orders, long totalMatches) {
}
