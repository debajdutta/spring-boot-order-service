package io.github.debajdutta.orderservice.order.search;


import java.util.Arrays;
import java.util.Optional;

/**
 * Public sort names ("orderBy=placed") mapped to internal sort fields.
 * Renaming the entity's createdAt would only change OrderSortField, never this API contract.
 */
enum OrderSortOption {

    PLACED("placed", OrderSortField.PLACED),
    TOTAL("total", OrderSortField.TOTAL),
    CUSTOMER("customer", OrderSortField.CUSTOMER),
    ITEM("item", OrderSortField.ITEM);

    private final String apiName;
    private final OrderSortField field;

    OrderSortOption(String apiName, OrderSortField field) {
        this.apiName = apiName;
        this.field = field;
    }

    public String apiName() {
        return apiName;
    }

    public OrderSortField field() {
        return field;
    }

    public static Optional<OrderSortOption> fromApiName(String name) {
        return Arrays.stream(values()).filter(o -> o.apiName.equalsIgnoreCase(name)).findFirst();
    }

    public static OrderSortOption of(OrderSortField field) {
        return Arrays.stream(values()).filter(o -> o.field == field).findFirst().orElseThrow();
    }

    public static String allowedValues() {
        return String.join(", ", Arrays.stream(values()).map(OrderSortOption::apiName).toList());
    }
}
