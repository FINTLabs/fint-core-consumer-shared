package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fint.relations.FintLinker;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EventEntityCache<T extends FintLinks & Serializable> {

    private final FintLinker<T> fintLinker;

    private static final int MAX_HOURS_OLD = 4;
    private static final int MILI_TO_HOURS = 60 * 60 * 1000;

    private final Map<String, EventEntityCacheElement<T>> entities;

    public EventEntityCache(FintLinker<T> fintLinker) {
        this.fintLinker = fintLinker;
        this.entities = new HashMap<>();
    }

    public void add(String corrId, T entity) {
        log.info("Adding new response with corrId: {}", corrId);
        fintLinker.mapLinks(entity);
        entities.put(corrId, new EventEntityCacheElement<T>(entity));
    }

    public T get(String corrId) {
        return entities.containsKey(corrId) ? entities.get(corrId).getEntity() : null;
    }

    @Scheduled(initialDelay = 600000, fixedDelay = 1800000)
    private void removeOld() {
        long currentTime = System.currentTimeMillis();
        entities.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > MAX_HOURS_OLD * MILI_TO_HOURS);
    }
}
