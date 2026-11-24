package io.github.debajdutta.orderservice.order.search;

/**
 * The only things the search query is allowed to ORDER BY.
 * Each constant carries a fixed JPQL expression written by us; user input never
 * reaches the ORDER BY clause, so there is no injection risk and no sorting on
 * unindexed or sensitive columns the API didn't intend to offer.
 */
enum OrderSortField {

    PLACED("o.createdAt"),
    TOTAL("o.quantity * o.unitPrice"),   // computed, not a column
    CUSTOMER("lower(o.customerName)"),
    ITEM("lower(o.product)");

    private final String jpqlExpression;

    OrderSortField(String jpqlExpression) {
        this.jpqlExpression = jpqlExpression;
    }

    public String jpqlExpression() {
        return jpqlExpression;
    }
}
