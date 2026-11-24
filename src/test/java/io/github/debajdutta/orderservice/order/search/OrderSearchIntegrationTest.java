package io.github.debajdutta.orderservice.order.search;

import io.github.debajdutta.orderservice.order.domain.Order;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import io.github.debajdutta.orderservice.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data set (total = quantity * unitPrice):
 *   Alice Johnson | Laptop Stand        | 2 x 49.99  =  99.98 | CREATED   -> open
 *   Bob Smith     | Mechanical Keyboard | 1 x 129.00 = 129.00 | CONFIRMED -> open
 *   Carol White   | USB-C Hub           | 3 x 34.50  = 103.50 | SHIPPED   -> in_transit
 *   alice cooper  | Desk Lamp           | 5 x 20.00  = 100.00 | CANCELLED -> cancelled
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderSearchIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    OrderRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.saveAll(List.of(
                order("Alice Johnson", "Laptop Stand", 2, "49.99", OrderStatus.CREATED),
                order("Bob Smith", "Mechanical Keyboard", 1, "129.00", OrderStatus.CONFIRMED),
                order("Carol White", "USB-C Hub", 3, "34.50", OrderStatus.SHIPPED),
                order("alice cooper", "Desk Lamp", 5, "20.00", OrderStatus.CANCELLED)));
    }

    private static Order order(String customer, String product, int qty, String price, OrderStatus status) {
        Order o = Order.create(customer, product, qty, new BigDecimal(price), "test");
        o.setStatus(status);
        return o;
    }

    @Test
    void noParametersReturnsEverythingWithDefaults() throws Exception {
        mvc.perform(get("/api/v1/orders/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(4))
                .andExpect(jsonPath("$.paging.totalMatches").value(4))
                .andExpect(jsonPath("$.paging.limit").value(20))
                .andExpect(jsonPath("$.sort.orderBy").value("placed"))
                .andExpect(jsonPath("$.sort.direction").value("desc"));
    }

    @Test
    void stateOpenMapsToCreatedAndConfirmed() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("state", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[*].state", everyItem(is("open"))));
    }

    @Test
    void customerMatchIsCaseInsensitiveContains() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("customer", "ALICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2));
    }

    @Test
    void wildcardCharactersAreMatchedLiterally() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("customer", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test
    void totalRangeFiltersOnComputedAmount() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("minTotal", "100").param("maxTotal", "110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2)); // 103.50 and 100.00
    }

    @Test
    void orderByTotalDescPutsLargestFirstAndMapsFields() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("orderBy", "total").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].customer").value("Bob Smith"))
                .andExpect(jsonPath("$.results[0].item").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.results[0].orderNumber", startsWith("ORD-")))
                .andExpect(jsonPath("$.results[0].href", startsWith("/api/v1/orders/")))
                .andExpect(jsonPath("$.results[3].customer").value("Alice Johnson")); // 99.98 smallest
    }

    @Test
    void limitAndOffsetDrivePaging() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("limit", "3").param("offset", "0"))
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.paging.hasMore").value(true));
        mvc.perform(get("/api/v1/orders/search").param("limit", "3").param("offset", "3"))
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.paging.hasMore").value(false));
    }

    @Test
    void placedDatesAreInclusiveCalendarDays() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        mvc.perform(get("/api/v1/orders/search").param("placedFrom", today.toString()).param("placedTo", today.toString()))
                .andExpect(jsonPath("$.results.length()").value(4));
        mvc.perform(get("/api/v1/orders/search").param("placedTo", today.minusDays(1).toString()))
                .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test
    void invalidParametersAreAllReportedTogether() throws Exception {
        mvc.perform(get("/api/v1/orders/search")
                        .param("state", "bogus")
                        .param("orderBy", "createdAt")   // entity field name is NOT a valid API name
                        .param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.state").exists())
                .andExpect(jsonPath("$.errors.orderBy").exists())
                .andExpect(jsonPath("$.errors.limit").exists());
    }

    @Test
    void unparseableValueIs400() throws Exception {
        mvc.perform(get("/api/v1/orders/search").param("placedFrom", "24-11-2026"))
                .andExpect(status().isBadRequest());
    }
}
