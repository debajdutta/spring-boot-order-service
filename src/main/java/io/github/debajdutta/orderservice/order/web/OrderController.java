package io.github.debajdutta.orderservice.order.web;

import io.github.debajdutta.orderservice.common.web.PageResponse;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.CreateOrderRequest;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.OrderResponse;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.UpdateOrderRequest;
import io.github.debajdutta.orderservice.order.dto.OrderDtos.UpdateStatusRequest;
import io.github.debajdutta.orderservice.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Orders REST API, version 1: /api/v1/orders (see config/ApiVersioningConfig).
 *
 * Phase 2 role mapping (Entra app roles):
 *   GET            -> Orders.Reader
 *   POST/PUT/PATCH -> Orders.Writer
 *   DELETE         -> Orders.Admin
 */
@RestController
@RequestMapping("/api/{version}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    /** GET /api/v1/orders?status=CREATED&page=0&size=20&sort=createdAt,desc */
    @GetMapping(version = "1")
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(service.list(status, pageable));
    }

    @GetMapping(path = "/{id}", version = "1")
    public OrderResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /** 201 Created with a Location header pointing at the new resource. */
    @PostMapping(version = "1")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(path = "/{id}", version = "1")
    public OrderResponse replace(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        return service.replace(id, request);
    }

    @PatchMapping(path = "/{id}/status", version = "1")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.changeStatus(id, request.status());
    }

    /** 204 No Content on success. */
    @DeleteMapping(path = "/{id}", version = "1")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
