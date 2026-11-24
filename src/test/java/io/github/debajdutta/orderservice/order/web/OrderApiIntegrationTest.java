package io.github.debajdutta.orderservice.order.web;

import io.github.debajdutta.orderservice.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end API tests against H2: controller -> service -> JPA -> DB. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    private static final String VALID_ORDER = """
            {"customerName":"Test User","product":"Monitor","quantity":2,"unitPrice":199.99}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    OrderRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private long createOrder() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void createReturns201WithLocationAndComputedTotal() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/orders/")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(399.98))
                .andExpect(jsonPath("$.createdBy").value("anonymous"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"","product":"X","quantity":0,"unitPrice":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.customerName").exists())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.unitPrice").exists());
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mvc.perform(get("/api/v1/orders/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void listIsPaged() throws Exception {
        createOrder();
        createOrder();
        mvc.perform(get("/api/v1/orders?size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void putReplacesFields() throws Exception {
        long id = createOrder();
        mvc.perform(put("/api/v1/orders/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Renamed","product":"Monitor","quantity":3,"unitPrice":100.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Renamed"))
                .andExpect(jsonPath("$.totalAmount").value(300.0));
    }

    @Test
    void statusTransitionsAreEnforced() throws Exception {
        long id = createOrder();
        // CREATED -> SHIPPED skips CONFIRMED: rejected
        mvc.perform(patch("/api/v1/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isConflict());
        // CREATED -> CONFIRMED: allowed
        mvc.perform(patch("/api/v1/orders/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        // Confirmed orders can no longer be edited with PUT
        mvc.perform(put("/api/v1/orders/" + id).contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204ThenGetReturns404() throws Exception {
        long id = createOrder();
        mvc.perform(delete("/api/v1/orders/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/orders/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void unknownJsonFieldIsRejected() throws Exception {
        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"A","product":"B","quantity":1,"unitPrice":1.00,"bogus":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedApiVersionReturns400() throws Exception {
        mvc.perform(get("/api/v2/orders")).andExpect(status().isBadRequest());
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
