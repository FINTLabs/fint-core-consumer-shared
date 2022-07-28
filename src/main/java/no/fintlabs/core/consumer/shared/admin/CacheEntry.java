package no.fintlabs.core.consumer.shared.admin;

import lombok.Data;

import java.util.Date;

@Data
public class CacheEntry {
    private final Date lastUpdated;
    private final Integer size;
}
