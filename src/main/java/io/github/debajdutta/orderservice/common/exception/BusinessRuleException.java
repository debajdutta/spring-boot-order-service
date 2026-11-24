package io.github.debajdutta.orderservice.common.exception;

/** A request that is well-formed but conflicts with the resource's current state. Mapped to HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
