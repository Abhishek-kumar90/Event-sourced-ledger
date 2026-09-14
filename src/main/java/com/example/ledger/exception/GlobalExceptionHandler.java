package com.example.ledger.exception;

import com.example.ledger.eventstore.ConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * One place that turns every failure into a safe HTTP response.
 *
 * <ul>
 *   <li>request validation / bad body -> 400</li>
 *   <li>invalid command (bad amount, self-transfer) -> 400</li>
 *   <li>account not found -> 404</li>
 *   <li>optimistic concurrency conflict -> 409</li>
 *   <li>insufficient funds (valid request, disallowed by business rule) -> 422</li>
 *   <li>anything unexpected -> 500 (generic message)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDATION_FAILED", "One or more fields are invalid.", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "MALFORMED_REQUEST",
                        "Request body could not be read. Check the JSON syntax.", null));
    }

    @ExceptionHandler(InvalidCommandException.class)
    public ResponseEntity<ApiError> handleInvalidCommand(InvalidCommandException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_COMMAND", ex.getMessage(), null));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "ACCOUNT_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<ApiError> handleConcurrency(ConcurrencyException ex) {
        // Expected under contention; the caller can reload and retry.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "CONCURRENCY_CONFLICT", ex.getMessage(), null));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "INSUFFICIENT_FUNDS", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR", "An unexpected error occurred.", null));
    }
}
