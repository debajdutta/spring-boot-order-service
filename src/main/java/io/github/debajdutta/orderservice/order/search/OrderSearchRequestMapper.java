package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.common.exception.InvalidRequestException;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Translates the API request (client vocabulary) into OrderSearchCriteria (domain vocabulary).
 * All defaults and rules for this endpoint live here, and ALL problems are reported
 * together in one 400 response instead of failing on the first one.
 */
@Component
class OrderSearchRequestMapper {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;
    static final int MAX_TEXT_LENGTH = 100;
    static final OrderSortOption DEFAULT_SORT = OrderSortOption.PLACED;

    public OrderSearchCriteria toCriteria(OrderSearchRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Text filters: trim, blank means "not given"
        String customer = cleanText(req.customer(), "customer", errors);
        String item = cleanText(req.item(), "item", errors);

        // state=open -> {CREATED, CONFIRMED}
        Set<OrderStatus> statuses = null;
        if (hasText(req.state())) {
            statuses = OrderState.fromApiValue(req.state().trim())
                    .map(OrderState::statuses)
                    .orElse(null);
            if (statuses == null) {
                errors.put("state", "must be one of: " + OrderState.allowedValues());
            }
        }

        // Totals
        BigDecimal minTotal = req.minTotal();
        BigDecimal maxTotal = req.maxTotal();
        if (minTotal != null && minTotal.signum() < 0) {
            errors.put("minTotal", "must be zero or more");
        }
        if (maxTotal != null && maxTotal.signum() < 0) {
            errors.put("maxTotal", "must be zero or more");
        }
        if (minTotal != null && maxTotal != null && minTotal.compareTo(maxTotal) > 0) {
            errors.put("minTotal", "must not be greater than maxTotal");
        }

        // Dates: the API speaks in calendar days (inclusive); the DB stores instants.
        //   placedFrom=2026-11-01 -> createdAt >= 2026-11-01T00:00Z
        //   placedTo=2026-11-24   -> createdAt <  2026-11-25T00:00Z  (so the whole 24th is included)
        LocalDate from = req.placedFrom();
        LocalDate to = req.placedTo();
        if (from != null && to != null && from.isAfter(to)) {
            errors.put("placedFrom", "must not be after placedTo");
        }
        Instant createdFrom = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant createdTo = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // Sort: only whitelisted names
        OrderSortOption sort = DEFAULT_SORT;
        if (hasText(req.orderBy())) {
            sort = OrderSortOption.fromApiName(req.orderBy().trim()).orElse(null);
            if (sort == null) {
                errors.put("orderBy", "must be one of: " + OrderSortOption.allowedValues());
                sort = DEFAULT_SORT;
            }
        }
        boolean descending = true; // newest / largest first unless asked otherwise
        if (hasText(req.direction())) {
            switch (req.direction().trim().toLowerCase()) {
                case "asc" -> descending = false;
                case "desc" -> descending = true;
                default -> errors.put("direction", "must be asc or desc");
            }
        }

        // Paging
        int limit = req.limit() == null ? DEFAULT_LIMIT : req.limit();
        int offset = req.offset() == null ? 0 : req.offset();
        if (limit < 1 || limit > MAX_LIMIT) {
            errors.put("limit", "must be between 1 and " + MAX_LIMIT);
        }
        if (offset < 0) {
            errors.put("offset", "must be zero or more");
        }

        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }

        return new OrderSearchCriteria(customer, item, statuses, minTotal, maxTotal,
                createdFrom, createdTo, sort.field(), descending, offset, limit);
    }

    private static String cleanText(String value, String param, Map<String, String> errors) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            errors.put(param, "must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
