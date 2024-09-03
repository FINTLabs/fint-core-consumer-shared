package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.FintEvent;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventCache<T extends FintEvent> {

    private static final int MAX_HOURS_OLD = 4;
    private final ConcurrentMap<String, EventWrapper<T>> responses = new ConcurrentHashMap<>();

    public void add(T fintEvent, String eventName) {
        log.info("Adding new {} with corrId: {}", eventName, fintEvent.getCorrId());
        responses.put(fintEvent.getCorrId(), new EventWrapper<T>(fintEvent));
    }

    public T get(String corrId) {
        return responses.containsKey(corrId) ? responses.get(corrId).getFintEvent() : null;
    }

    @Scheduled(fixedDelay = 30, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
    private void purgeExpiredEvents() {
        long currentTime = System.currentTimeMillis();
        long maxAgeMillis = TimeUnit.HOURS.toMillis(MAX_HOURS_OLD);
        responses.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > maxAgeMillis);
    }
}
