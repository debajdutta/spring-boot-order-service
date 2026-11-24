package io.github.debajdutta.orderservice.common.web;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable JSON shape for paged results. Serializing Spring Data's Page directly
 * is discouraged because its JSON structure is not a guaranteed contract.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
