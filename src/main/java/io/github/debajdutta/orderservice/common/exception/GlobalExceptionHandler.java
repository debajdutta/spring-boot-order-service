package io.github.debajdutta.orderservice.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into RFC 9457 "Problem Details" JSON
 * (type, title, status, detail, instance + extra properties).
 * Extending ResponseEntityExceptionHandler covers Spring MVC's own errors
 * (malformed JSON, wrong method, unsupported media type) in the same format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        return pd;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Business rule violation");
        return pd;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The order was modified by another request. Reload and retry.");
        pd.setTitle("Concurrent modification");
        return pd;
    }

    /** Query parameters that parsed fine but break the endpoint's rules (e.g. search). */
    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more parameters are invalid");
        pd.setTitle("Invalid request");
        pd.setProperty("errors", ex.getErrors());
        return pd;
    }

    /** Bean Validation failures on @Valid request bodies: 400 with a field -> message map. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setTitle("Validation error");
        pd.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().body(pd);
    }

    /** Safety net: never leak stack traces to the client. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        pd.setTitle("Internal error");
        return pd;
    }
}
