package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventEntityCache<T extends FintLinks & Serializable> {

    private static final int MAX_HOURS_OLD = 4;
    private final FintLinker<T> fintLinker;
    private final ConcurrentMap<String, EventEntityCacheElement<T>> entities;

    public EventEntityCache(FintLinker<T> fintLinker) {
        this.fintLinker = fintLinker;
        this.entities = new ConcurrentHashMap<>();
    }

    public void add(String corrId, T entity) {
        log.info("Adding new response with corrId: {}", corrId);
        fintLinker.mapLinks(entity);
        entities.put(corrId, new EventEntityCacheElement<T>(entity));
    }

    public T get(String corrId) {
        return entities.containsKey(corrId) ? entities.get(corrId).getEntity() : null;
    }

    @Scheduled(fixedDelay = 30, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
    private void purgeOldEntries() {
        long currentTime = System.currentTimeMillis();
        long maxAgeMillis = TimeUnit.HOURS.toMillis(MAX_HOURS_OLD);
        entities.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > maxAgeMillis);
    }
}
