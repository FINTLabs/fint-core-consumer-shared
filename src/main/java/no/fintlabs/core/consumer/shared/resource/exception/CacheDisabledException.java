package no.fintlabs.core.consumer.shared.resource.exception;

public class CacheDisabledException extends RuntimeException {
    public CacheDisabledException(String message) {
        super(message);
    }
}
