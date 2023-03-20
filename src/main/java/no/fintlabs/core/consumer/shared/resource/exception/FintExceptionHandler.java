package no.fintlabs.core.consumer.shared.resource.exception;

import no.fint.antlr.exception.FilterException;
import no.fint.cache.exceptions.CacheNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;

@ControllerAdvice
@RestController
public class FintExceptionHandler {

    @ExceptionHandler(FilterException.class)
    public ResponseEntity<ErrorResponse> handleFilterException(FilterException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, "FILTER_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EventResponseException.class)
    public ResponseEntity<ErrorResponse> handleEventResponseException(EventResponseException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getStatus(), "EVENT_RESPONSE_ERROR", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(UpdateEntityMismatchException.class)
    public ResponseEntity<ErrorResponse> handleUpdateEntityMismatch(UpdateEntityMismatchException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, "UPDATE_ENTITY_MISMATCH_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(CreateEntityMismatchException.class)
    public ResponseEntity<ErrorResponse> handleCreateEntityMismatch(CreateEntityMismatchException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, "CREATE_ENTITY_MISMATCH_ERROR", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(EntityFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityFound(EntityFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FOUND, "ENTITY_FOUND_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.FOUND).body(errorResponse);
    }

    @ExceptionHandler(CacheDisabledException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(CacheDisabledException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "CACHE_DISABLED_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity<ErrorResponse> handleUnkownHost(UnknownHostException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "UNKNOWN_HOST_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(CacheNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCacheNotFound(CacheNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "CACHE_NOT_FOUND_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

}
