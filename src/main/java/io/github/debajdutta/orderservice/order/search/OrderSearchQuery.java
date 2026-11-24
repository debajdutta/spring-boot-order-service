package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.order.domain.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hand-written search query - no method-name magic. Owned by the search slice:
 * nothing outside this package uses it, so it does not live in the shared OrderRepository.
 *
 * Builds one JPQL WHERE clause from whichever filters are present, then runs:
 *   1. a data query  (WHERE + ORDER BY + offset/limit)  -> the rows
 *   2. a count query (same WHERE, no ORDER BY)          -> total matches
 * Every value goes in as a bound parameter (":name"), never concatenated,
 * so user input cannot change the SQL. Only our own whitelisted
 * OrderSortField expressions are concatenated into ORDER BY.
 */
@Repository
class OrderSearchQuery {

    /** LIKE escape character. '!' instead of '\' because MySQL treats '\' specially in string literals. */
    private static final char LIKE_ESCAPE = '!';

    @PersistenceContext
    private EntityManager entityManager;

    public OrderSearchResult search(OrderSearchCriteria c) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new LinkedHashMap<>();

        if (c.customerContains() != null) {
            where.append(" and lower(o.customerName) like :customer escape '" + LIKE_ESCAPE + "'");
            params.put("customer", containsPattern(c.customerContains()));
        }
        if (c.productContains() != null) {
            where.append(" and lower(o.product) like :product escape '" + LIKE_ESCAPE + "'");
            params.put("product", containsPattern(c.productContains()));
        }
        if (c.statuses() != null && !c.statuses().isEmpty()) {
            where.append(" and o.status in :statuses");
            params.put("statuses", c.statuses());
        }
        if (c.minTotal() != null) {
            where.append(" and (o.quantity * o.unitPrice) >= :minTotal");
            params.put("minTotal", c.minTotal());
        }
        if (c.maxTotal() != null) {
            where.append(" and (o.quantity * o.unitPrice) <= :maxTotal");
            params.put("maxTotal", c.maxTotal());
        }
        if (c.createdFromInclusive() != null) {
            where.append(" and o.createdAt >= :createdFrom");
            params.put("createdFrom", c.createdFromInclusive());
        }
        if (c.createdToExclusive() != null) {
            where.append(" and o.createdAt < :createdTo");
            params.put("createdTo", c.createdToExclusive());
        }

        // Tie-breaker on id keeps paging stable when sort values are equal.
        String direction = c.descending() ? " desc" : " asc";
        String orderBy = " order by " + c.sortField().jpqlExpression() + direction + ", o.id" + direction;

        // 1. Data query
        TypedQuery<Order> dataQuery = entityManager.createQuery(
                "select o from Order o" + where + orderBy, Order.class);
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult(c.offset());
        dataQuery.setMaxResults(c.limit());
        List<Order> rows = dataQuery.getResultList();

        // 2. Count query
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(o) from Order o" + where, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        return new OrderSearchResult(rows, total);
    }

    /** "50%_off" -> "%50!%!_off%" so user-typed % and _ are matched literally, not as wildcards. */
    private static String containsPattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
