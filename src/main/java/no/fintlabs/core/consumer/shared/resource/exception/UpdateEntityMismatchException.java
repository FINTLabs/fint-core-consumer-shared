package no.fintlabs.core.consumer.shared.resource.exception;

@SuppressWarnings("ALL")
public class UpdateEntityMismatchException extends RuntimeException {
    public UpdateEntityMismatchException(String message) {
        super(message);
    }
}
