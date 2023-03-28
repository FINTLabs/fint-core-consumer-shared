package no.fintlabs.core.consumer.shared.resource.exception;

import java.util.NoSuchElementException;

public class CacheNotFoundException extends NoSuchElementException {
    public CacheNotFoundException(String s) {
        super(s);
    }
}
