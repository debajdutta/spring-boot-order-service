package io.github.debajdutta.orderservice.common.exception;

import java.util.Map;

/**
 * Request parameters that are syntactically fine but not acceptable
 * (unknown sort field, limit too large, min > max ...). Mapped to HTTP 400
 * with the same "errors" map shape as Bean Validation failures.
 */
public class InvalidRequestException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidRequestException(Map<String, String> errors) {
        super("Invalid request parameters: " + errors.keySet());
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
