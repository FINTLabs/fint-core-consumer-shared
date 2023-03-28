package no.fintlabs.core.consumer.shared.resource.exception;

import no.fint.event.model.EventResponse;
import org.springframework.http.HttpStatus;

public class EventResponseException extends RuntimeException {
    private final HttpStatus status;
    private final EventResponse response;

    public EventResponseException(HttpStatus status, EventResponse response) {
        super(response.getMessage());
        this.status = status;
        this.response = response;
    }

    public EventResponse getResponse() {
        return response;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
