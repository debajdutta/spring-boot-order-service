package io.github.debajdutta.orderservice.common.exception;

/** Mapped to HTTP 404 by GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
    }
}
