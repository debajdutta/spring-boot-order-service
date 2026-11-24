package io.github.debajdutta.orderservice.order.search;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * GET /api/v1/orders/search
 *
 * Example:
 *   /api/v1/orders/search?customer=alice&state=open&minTotal=50&placedFrom=2026-11-01
 *                      &orderBy=total&direction=desc&limit=10&offset=0
 *
 * Every parameter is named explicitly with @RequestParam(name = ...), so the API names are
 * visible here and independent of the entity. Spring only converts types
 * (String -> BigDecimal / LocalDate / Integer; a bad value gives 400); all meaning is
 * applied in OrderSearchRequestMapper.
 *
 * Phase 2 role: Orders.Reader
 */
@RestController
@RequestMapping("/api/{version}/orders/search")
@RequiredArgsConstructor
public class OrderSearchController {

    private final OrderSearchService searchService;

    @GetMapping(version = "1")
    public OrderSearchResponse search(
            @RequestParam(name = "customer", required = false) String customer,
            @RequestParam(name = "item", required = false) String item,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "minTotal", required = false) BigDecimal minTotal,
            @RequestParam(name = "maxTotal", required = false) BigDecimal maxTotal,
            @RequestParam(name = "placedFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate placedFrom,
            @RequestParam(name = "placedTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate placedTo,
            @RequestParam(name = "orderBy", required = false) String orderBy,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset) {

        OrderSearchRequest request = new OrderSearchRequest(
                customer, item, state, minTotal, maxTotal, placedFrom, placedTo,
                orderBy, direction, limit, offset);
        return searchService.search(request);
    }
}
