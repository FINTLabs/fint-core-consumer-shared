package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EventEntityCache {

    private static final int MAX_HOURS_OLD = 4;
    private static final int MILI_TO_HOURS = 60 * 60 * 1000;

    private final Map<String, EventEntityCacheElement> entities;

    public EventEntityCache() {
        this.entities = new HashMap<>();
    }

    public void add(String corrId, Object entity) {
        log.info("Adding new response with corrId: {}", corrId);
        entities.put(corrId,new EventEntityCacheElement(entity));
    }

    public Object get(String corrId) {
        if (entities.containsKey(corrId)) {
            return entities.get(corrId).getEntity();
        }
        return null;
    }

    @Scheduled(initialDelay = 600000, fixedDelay = 1800000)
    private void removeOld() {
        long currentTime = System.currentTimeMillis();
        entities.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > MAX_HOURS_OLD * MILI_TO_HOURS);
    }
}
