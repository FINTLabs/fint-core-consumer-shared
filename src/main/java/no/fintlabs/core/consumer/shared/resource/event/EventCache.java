package no.fintlabs.core.consumer.shared.resource.event;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.adapter.models.FintEvent;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EventCache<T extends FintEvent> {

    private static final int MAX_HOURS_OLD = 4;
    private static final int MILI_TO_HOURS = 60 * 60 * 1000;

    private final Map<String, EventWrapper<T>> responses;

    public EventCache() {
        this.responses = new HashMap<>();
    }

    public void add(T fintEvent) {
        log.info("Adding new response with corrId: {}", fintEvent.getCorrId());
        responses.put(fintEvent.getCorrId(), new EventWrapper<T>(fintEvent));
    }

    public T get(String corrId) {
        if (responses.containsKey(corrId)) {
            return responses.get(corrId).getFintEvent();
        }
        return null;
    }

    @Scheduled(initialDelay = 600000, fixedDelay = 1800000)
    private void removeOld() {
        long currentTime = System.currentTimeMillis();
        responses.entrySet().removeIf(e -> currentTime - e.getValue().getCreated().getTime() > MAX_HOURS_OLD * MILI_TO_HOURS);
    }

}
